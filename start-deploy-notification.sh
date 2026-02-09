#!/bin/bash
set -e

BACKEND_COMPOSE=docker-compose.build.yml
KAFKA_COMPOSE=docker-compose.yml

echo "🚀 Starting Kafka stack..."
docker compose -f $KAFKA_COMPOSE up -d

echo "⏳ Waiting for Kafka to be ready..."
sleep 10

echo "🛑 Stopping old backend..."
docker compose -f $BACKEND_COMPOSE down || true

echo "🔨 Building backend..."
docker compose -f $BACKEND_COMPOSE build backend

echo "▶️ Starting backend..."
docker compose -f $BACKEND_COMPOSE up -d backend

echo "✅ Backend is running on port 6060"
