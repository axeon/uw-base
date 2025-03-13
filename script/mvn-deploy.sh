#!/bin/bash

# 直接计算项目根目录（简化路径获取）
BASE_DIR=$(dirname "$(dirname "$(readlink -f "$0")")")

# 切换到根目录执行命令
cd "$BASE_DIR" || (echo "Error: Failed to switch to project root directory: $BASE_DIR" && exit 1)

# 调试输出（可删除）
echo "INFO: BASE_DIR = $BASE_DIR"
echo "INFO: Current directory = $(pwd)"

# 帮助信息
usage() {
    echo "Usage: $0 [module1,module2,...]"
    echo "Example:"
    echo "  Deploy all modules: $0"
    echo "  Deploy specific modules: $0 uw-auth-service,uw-common"
    exit 1
}

# 初始化基础命令
CMD_DEPLOY="mvn deploy -P release -DskipTests"
CMD_CLEAN="mvn clean"

# 检查参数是否存在
if [ ! -z "$1" ]; then
    IFS=',' read -ra MODULES <<< "$1"
    PL_PARAMS=":uw-base"
    for module in "${MODULES[@]}"; do
        PL_PARAMS+=",$module"
    done
    CMD_DEPLOY+=" -pl \"$PL_PARAMS\""
fi

# 执行命令（clean -> deploy -> clean）
echo "Executing in $BASE_DIR:"
echo "1/3: Cleaning project..."
eval "$CMD_CLEAN"

echo "2/3: Deploying..."
eval "$CMD_DEPLOY"

echo "3/3: Final clean..."
eval "$CMD_CLEAN"
