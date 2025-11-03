#!/bin/bash

# Track 数据库恢复脚本
# 使用方法：bash scripts/restore.sh <备份文件路径>
# 示例：bash scripts/restore.sh backups/track_backup_20241103_120000.sql

set -e

echo "================================"
echo "Track 数据库恢复脚本"
echo "================================"
echo ""

# 检查参数
if [ -z "$1" ]; then
    echo "❌ 错误：请提供备份文件路径"
    echo "使用方法：bash scripts/restore.sh <备份文件路径>"
    echo ""
    echo "可用的备份文件："
    if [ -d "./backups" ]; then
        ls -lh ./backups/*.sql 2>/dev/null || echo "  （无备份文件）"
    else
        echo "  （无备份目录）"
    fi
    exit 1
fi

BACKUP_FILE=$1

# 检查备份文件是否存在
if [ ! -f "$BACKUP_FILE" ]; then
    echo "❌ 错误：备份文件不存在：$BACKUP_FILE"
    exit 1
fi

echo "⚠️  警告：此操作将覆盖当前数据库中的所有数据！"
echo "备份文件：$BACKUP_FILE"
echo ""
read -p "确认要恢复吗？(yes/no): " confirm

if [ "$confirm" != "yes" ]; then
    echo "取消恢复操作"
    exit 0
fi

echo ""
echo "📦 开始恢复数据库..."

# 从 .env 文件读取数据库配置
if [ -f ".env" ]; then
    export $(cat .env | grep -v '^#' | xargs)
fi

POSTGRES_USER=${POSTGRES_USER:-postgres}
POSTGRES_DB=${POSTGRES_DB:-track}

# 执行恢复
cat $BACKUP_FILE | docker compose exec -T postgres psql -U $POSTGRES_USER $POSTGRES_DB

if [ $? -eq 0 ]; then
    echo "✅ 恢复成功！"
else
    echo "❌ 恢复失败！"
    exit 1
fi

echo ""
echo "================================"
echo "恢复完成！"
echo "================================"

