#!/bin/bash

# Track 数据库备份脚本
# 使用方法：bash scripts/backup.sh

set -e

echo "================================"
echo "Track 数据库备份脚本"
echo "================================"
echo ""

# 创建备份目录
BACKUP_DIR="./backups"
mkdir -p $BACKUP_DIR

# 生成备份文件名（包含时间戳）
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
BACKUP_FILE="$BACKUP_DIR/track_backup_$TIMESTAMP.sql"

echo "📦 开始备份数据库..."
echo "备份文件：$BACKUP_FILE"
echo ""

# 从 .env 文件读取数据库配置
if [ -f ".env" ]; then
    export $(cat .env | grep -v '^#' | xargs)
fi

POSTGRES_USER=${POSTGRES_USER:-postgres}
POSTGRES_DB=${POSTGRES_DB:-track}

# 执行备份
docker compose exec -T postgres pg_dump -U $POSTGRES_USER $POSTGRES_DB > $BACKUP_FILE

# 检查备份是否成功
if [ -f "$BACKUP_FILE" ] && [ -s "$BACKUP_FILE" ]; then
    FILE_SIZE=$(du -h "$BACKUP_FILE" | cut -f1)
    echo "✅ 备份成功！"
    echo "文件大小：$FILE_SIZE"
    echo "文件路径：$BACKUP_FILE"
    
    # 列出所有备份文件
    echo ""
    echo "📋 现有备份文件："
    ls -lh $BACKUP_DIR/*.sql
    
    # 清理超过 7 天的备份
    echo ""
    echo "🧹 清理超过 7 天的旧备份..."
    find $BACKUP_DIR -name "*.sql" -type f -mtime +7 -delete
    echo "✅ 清理完成"
else
    echo "❌ 备份失败！"
    exit 1
fi

echo ""
echo "================================"
echo "备份完成！"
echo "================================"

