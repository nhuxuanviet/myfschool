FROM ghcr.io/cirruslabs/flutter:3.41.9 AS build
WORKDIR /app
ARG API_BASE_URL
COPY pubspec.yaml pubspec.lock ./
RUN flutter pub get
COPY . .
RUN test -n "$API_BASE_URL" && flutter build web --release --dart-define=API_BASE_URL="$API_BASE_URL"

FROM nginxinc/nginx-unprivileged:1.28-alpine
COPY deploy/student-web.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/build/web /usr/share/nginx/html
EXPOSE 8080
