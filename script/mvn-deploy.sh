#!/bin/bash

# 直接计算项目根目录（兼容 macOS 自带 readlink 与 GNU readlink）
script_path="$0"
if [ -L "$script_path" ]; then
    link_dir=$(dirname "$script_path")
    link_target=$(readlink "$script_path")
    case "$link_target" in
        /*) script_path="$link_target" ;;
        *)  script_path="$link_dir/$link_target" ;;
    esac
fi
BASE_DIR=$(cd "$(dirname "$script_path")/.." && pwd)
cd "$BASE_DIR" || (echo "Error: Failed to switch to project root directory: $BASE_DIR" && exit 1)

echo "INFO: BASE_DIR = $BASE_DIR"

usage() {
    echo "Usage: $0 -a|--all | [--skip-parent] module1 [module2 ...]"
    echo ""
    echo "Options:"
    echo "  -a, --all          全量发布所有模块"
    echo "  --skip-parent      跳过uw-base parent发布（补发模块时使用）"
    echo "  -h, --help         显示帮助信息"
    echo ""
    echo "Example:"
    echo "  Deploy all:        $0 -a"
    echo "  Deploy modules:    $0 uw-auth-client uw-common"
    echo "  Redeploy modules:  $0 --skip-parent uw-common-app uw-httpclient"
    exit 0
}

# 无参数时显示帮助
[ $# -eq 0 ] && usage

# 初始化
SKIP_PARENT=false
DEPLOY_ALL=false
MODULES_ARGS=()

# 解析参数
for arg in "$@"; do
    case "$arg" in
        -h|--help) usage ;;
        -a|--all) DEPLOY_ALL=true ;;
        --skip-parent) SKIP_PARENT=true ;;
        *) MODULES_ARGS+=("$arg") ;;
    esac
done

# -a和模块名不能同时指定
if [ "$DEPLOY_ALL" = true ] && [ ${#MODULES_ARGS[@]} -gt 0 ]; then
    echo "Error: -a/--all cannot be used with module names"
    exit 1
fi

# -a和--skip-parent不能同时指定
if [ "$DEPLOY_ALL" = true ] && [ "$SKIP_PARENT" = true ]; then
    echo "Error: -a/--all cannot be used with --skip-parent"
    exit 1
fi

# 构建deploy命令
CMD_DEPLOY="mvn deploy -P release-uw -Dmaven.test.skip=true"
# clean 不加 -q：一旦出错需要完整日志定位；clean 始终全量 reactor，不追加 -pl，
# 避免只清理选中模块而残留其它模块的 .flattened-pom.xml
CMD_CLEAN="mvn clean -P release-uw"

if [ "$DEPLOY_ALL" = false ] && [ ${#MODULES_ARGS[@]} -gt 0 ]; then
    PL_PARAMS=""
    if [ "$SKIP_PARENT" = false ]; then
        PL_PARAMS=":uw-base"
    fi
    for arg in "${MODULES_ARGS[@]}"; do
        IFS=',' read -ra PARTS <<< "$arg"
        for module in "${PARTS[@]}"; do
            [ -n "$PL_PARAMS" ] && PL_PARAMS+=","
            PL_PARAMS+="$module"
        done
    done
    CMD_DEPLOY+=" -pl $PL_PARAMS"
fi

# 执行
echo "1/2: Cleaning project..."
eval "$CMD_CLEAN" || exit 1

echo "2/2: Deploying..."
if ! eval "$CMD_DEPLOY"; then
    # deploy 失败时清理本次 deploy 过程中新生成的 .flattened-pom.xml
    # （clean 阶段已清过旧的；flatten goal 在 process-resources 阶段会重新生成，归因见上）
    find "$BASE_DIR" -name ".flattened-pom.xml" -type f -delete
    echo "ERROR: Deploy failed, cleaned up leftover .flattened-pom.xml files"
    exit 1
fi
