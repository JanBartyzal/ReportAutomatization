# MS-GW Dockerfile
# Nginx-based API Gateway for ReportAutomatization

FROM nginx:alpine

# Copy custom nginx configuration
COPY nginx.conf /etc/nginx/nginx.conf

# Copy static HTML files (docs landing page)
COPY html/ /usr/share/nginx/html/

# Create directories for logs and certificates
RUN mkdir -p /var/log/nginx /etc/nginx/certs

# Expose HTTP and HTTPS ports
EXPOSE 80 443

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
    CMD wget -q --spider http://localhost/health || exit 1

# Start nginx
CMD ["nginx", "-g", "daemon off;"]
