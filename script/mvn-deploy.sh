#!/bin/bash

# 直接计算项目根目录
BASE_DIR=$(dirname "$(dirname "$(readlink -f "$0")")")
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
CMD_CLEAN="mvn clean -P release-uw -q"

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
eval "$CMD_DEPLOY" || exit 1
