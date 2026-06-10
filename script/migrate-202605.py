#!/usr/bin/env python3
"""
uw-dao API 重构 - 批量迁移脚本
========================================
使用方法: python migrate-202605.py [项目根目录]
默认根目录为脚本所在目录的上级目录。
建议先在单个模块测试，确认无误后再全量执行。
========================================
"""

import os
import re
import sys

# ===== 配置 =====

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
DEFAULT_WORK_DIR = os.path.dirname(os.path.dirname(SCRIPT_DIR))

WORK_DIR = sys.argv[1] if len(sys.argv) > 1 else DEFAULT_WORK_DIR

# 排除的目录
EXCLUDE_DIRS = {"target", ".git", ".idea", "node_modules"}

# ===== 替换规则 =====

IMPORT_RULES = [
    (r'import uw\.common\.dto\.ResponseCode;', 'import uw.common.response.ResponseCode;'),
    (r'import uw\.common\.dto\.ResponseData;', 'import uw.common.response.ResponseData;'),
    (r'import uw\.dao\.QueryParam;', 'import uw.common.dto.QueryParam;'),
    (r'import uw\.dao\.PageQueryParam;', 'import uw.common.dto.PageQueryParam;'),
    (r'import uw\.dao\.DataList;', 'import uw.common.data.PageList;'),
    (r'import uw\.dao\.DataSet;', 'import uw.common.data.PageRowSet;'),
    (r'import uw\.log\.es\.vo\.ESDataList;', 'import uw.common.data.PageList;'),
]

WORD_RULES = [
    (r'\bDataList\b', 'PageList'),
    (r'\bESDataList\b', 'PageList'),
    (r'\bDataSet\b', 'PageRowSet'),
    (r'\bqueryForSingleValue\b', 'queryForValue'),
    (r'\bqueryForSingleList\b', 'queryForValueList'),
    (r'\bqueryForSingleObject\b', 'queryForObject'),
    (r'\bqueryForDataSet\b', 'queryForRowSet'),
    (r'\bselectForDataSet\b', 'selectForRowSet'),
    (r'\bexecuteCommand\b', 'execute'),
]

# 旧类被删除后，使用 import uw.dao.* 的文件需要补充显式 import
# 格式: (检测用的类名引用, 需要添加的 import)
WILDCARD_FIXES = [
    ('QueryParam', 'import uw.common.dto.QueryParam;'),
    ('PageQueryParam', 'import uw.common.dto.PageQueryParam;'),
    ('DataList', 'import uw.common.data.PageList;'),
    ('DataSet', 'import uw.common.data.PageRowSet;'),
]


def find_java_files(root_dir):
    java_files = []
    for dirpath, dirnames, filenames in os.walk(root_dir):
        dirnames[:] = [d for d in dirnames if d not in EXCLUDE_DIRS]
        for filename in filenames:
            if filename.endswith('.java'):
                java_files.append(os.path.join(dirpath, filename))
    return java_files


def apply_import_rules(content):
    for pattern, replacement in IMPORT_RULES:
        content = re.sub(pattern, replacement, content)
    return content


def apply_word_rules(content):
    for pattern, replacement in WORD_RULES:
        content = re.sub(pattern, replacement, content)
    return content


def fix_wildcard_imports(content):
    """对使用 import uw.dao.* 的文件，检测并补充缺失的显式 import"""
    if 'import uw.dao.*;' not in content:
        return content

    lines = content.split('\n')
    new_imports = []

    for class_name, import_line in WILDCARD_FIXES:
        if import_line in content:
            continue
        # 检测代码中是否使用了该类名（排除 import 行和注释）
        pattern = r'\b' + class_name + r'\b'
        in_code = False
        for line in lines:
            stripped = line.strip()
            if stripped.startswith('import ') or stripped.startswith('//') or stripped.startswith('*') or stripped.startswith('/*'):
                continue
            if re.search(pattern, line):
                in_code = True
                break
        if in_code:
            new_imports.append(import_line)

    if not new_imports:
        return content

    # 在 import uw.dao.* 之后插入新的 import
    result_lines = []
    for line in lines:
        result_lines.append(line)
        if line.strip() == 'import uw.dao.*;':
            for imp in new_imports:
                result_lines.append(imp)
    return '\n'.join(result_lines)


def dedup_imports(content):
    lines = content.split('\n')
    seen_imports = set()
    result = []
    in_import_section = False
    import_section_ended = False

    for line in lines:
        stripped = line.strip()
        if import_section_ended:
            result.append(line)
        elif stripped.startswith('import '):
            in_import_section = True
            if stripped not in seen_imports:
                seen_imports.add(stripped)
                result.append(line)
        elif in_import_section and stripped == '':
            result.append(line)
        elif in_import_section:
            import_section_ended = True
            result.append(line)
        else:
            result.append(line)

    return '\n'.join(result)


def process_file(filepath):
    with open(filepath, 'r', encoding='utf-8') as f:
        original = f.read()

    content = original
    content = apply_import_rules(content)
    content = apply_word_rules(content)
    content = fix_wildcard_imports(content)
    content = dedup_imports(content)

    if content != original:
        with open(filepath, 'w', encoding='utf-8') as f:
            f.write(content)
        return True
    return False


def main():
    print("=" * 48)
    print("uw-dao API 重构 - 批量迁移脚本")
    print("=" * 48)
    print(f"工作目录: {WORK_DIR}")
    print()

    if not os.path.isdir(WORK_DIR):
        print(f"错误: 目录不存在 - {WORK_DIR}")
        sys.exit(1)

    java_files = find_java_files(WORK_DIR)
    print(f"找到 {len(java_files)} 个 Java 文件")
    print()

    changed_count = 0
    for filepath in java_files:
        try:
            if process_file(filepath):
                changed_count += 1
                relpath = os.path.relpath(filepath, WORK_DIR)
                print(f"  已修改: {relpath}")
        except Exception as e:
            relpath = os.path.relpath(filepath, WORK_DIR)
            print(f"  错误: {relpath} - {e}")

    print()
    print("=" * 48)
    print(f"迁移完成！共修改 {changed_count}/{len(java_files)} 个文件")
    print("=" * 48)
    print()
    print("本次迁移包含以下重命名:")
    print("  - import: ResponseCode/ResponseData 移至 uw.common.response")
    print("  - import: QueryParam/PageQueryParam 移至 uw.common.dto")
    print("  - DataList -> PageList")
    print("  - ESDataList -> PageList")
    print("  - DataSet -> PageRowSet")
    print("  - queryForSingleValue -> queryForValue")
    print("  - queryForSingleList -> queryForValueList")
    print("  - queryForSingleObject -> queryForObject")
    print("  - queryForDataSet -> queryForRowSet")
    print("  - executeCommand -> execute")
    print()
    print("注意事项:")
    print("1. 旧类文件 DataList.java/DataSet.java/QueryParam.java")
    print("   /PageQueryParam.java/ESDataList.java 已删除")
    print("2. 使用 import uw.dao.* 的文件会自动补充缺失的显式 import")
    print("3. 新增方法 queryForList() 委托调用 list()")
    print("4. SQLCommandImpl 已改为手动从 ResultSet 提取数据")
    print("5. 建议执行后用 IDE 编译检查，确认无遗漏")


if __name__ == '__main__':
    main()
