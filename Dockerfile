FROM node:22-bookworm-slim AS frontend-build
WORKDIR /workspace
RUN corepack enable
COPY package.json pnpm-workspace.yaml pnpm-lock.yaml ./
COPY frontend/package.json frontend/package.json
RUN pnpm install --frozen-lockfile
COPY frontend ./frontend
RUN pnpm --dir frontend build

FROM maven:3.9.9-eclipse-temurin-21 AS backend-build
WORKDIR /workspace
COPY pom.xml ./pom.xml
COPY backend ./backend
COPY --from=frontend-build /workspace/frontend/dist ./backend/src/main/resources/static
RUN mvn -B -f backend/pom.xml -DskipTests package

FROM eclipse-temurin:21-jre-jammy AS runtime
WORKDIR /app
COPY --from=backend-build /workspace/backend/target/taskflow-backend-1.0.0.jar /app/taskflow.jar
ENV PORT=3000
EXPOSE 3000
ENTRYPOINT ["java", "-jar", "/app/taskflow.jar"]
