# Build stage
FROM node:20 AS build
WORKDIR /app
COPY package*.json ./
RUN npm install
COPY . .

# Build-time API base URL (must be provided when building)
ARG REACT_APP_API_URL
ENV REACT_APP_API_URL=${REACT_APP_API_URL}

# Optional: show which API URL is baked into the build
RUN echo "Building frontend with REACT_APP_API_URL=${REACT_APP_API_URL}"

RUN npm run build

# Production stage
FROM nginx:alpine

# Add SPA-friendly nginx config to support client-side routes
COPY nginx.conf /etc/nginx/conf.d/default.conf

COPY --from=build /app/build /usr/share/nginx/html
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
