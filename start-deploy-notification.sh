#!/bin/bash
set -e

KAFKA_COMPOSE=docker-compose.yml

echo "🚀 Starting Kafka stack..."
docker compose -f $KAFKA_COMPOSE up -d zookeeper kafka kafka-ui

echo "⏳ Waiting for Kafka to be ready..."
sleep 15

echo "🛑 Stopping backend if running..."
docker compose -f $KAFKA_COMPOSE down backend || true

echo "🔨 Building backend..."
docker compose -f $KAFKA_COMPOSE build backend

echo "▶️ Starting backend..."
docker compose -f $KAFKA_COMPOSE up -d backend

echo "✅ Backend running at http://localhost:6060"
echo "✅ Kafka UI running at http://localhost:8090"
echo ""
echo "📋 Check backend logs with: docker logs -f notification_service"