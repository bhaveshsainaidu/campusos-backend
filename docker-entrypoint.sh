#!/usr/bin/env bash
set -e

# If an external DB_URL is not specified, run built-in MySQL server
if [ -z "$DB_URL" ]; then
    echo "========================================================"
    echo "  CampusOS: Starting Built-in MySQL Server on Render"
    echo "========================================================"
    
    mkdir -p /var/run/mysqld /var/lib/mysql
    chown -R mysql:mysql /var/run/mysqld /var/lib/mysql

    # Initialize data dir if empty
    if [ ! -d "/var/lib/mysql/mysql" ]; then
        mysqld --initialize-insecure --user=mysql
    fi

    # Start MySQL in background with low memory footprint
    mysqld_safe --skip-networking=0 --bind-address=127.0.0.1 --performance-schema=OFF --innodb-buffer-pool-size=32M &

    # Wait for MySQL to be ready
    echo "Waiting for MySQL to boot..."
    for i in $(seq 1 30); do
        if mysqladmin ping -h 127.0.0.1 --silent 2>/dev/null; then
            break
        fi
        sleep 1
    done

    # Create database and grant permissions
    mysql -h 127.0.0.1 -u root -e "CREATE DATABASE IF NOT EXISTS campusos; CREATE USER IF NOT EXISTS 'root'@'localhost' IDENTIFIED BY 'campusos'; GRANT ALL PRIVILEGES ON *.* TO 'root'@'localhost' WITH GRANT OPTION; CREATE USER IF NOT EXISTS 'root'@'127.0.0.1' IDENTIFIED BY 'campusos'; GRANT ALL PRIVILEGES ON *.* TO 'root'@'127.0.0.1' WITH GRANT OPTION; FLUSH PRIVILEGES;" 2>/dev/null || true

    echo "Built-in MySQL is ready on 127.0.0.1:3306! Booting Spring Boot..."
    export MYSQL_HOST=127.0.0.1
    export MYSQL_PORT=3306
    export MYSQL_DB=campusos
    export MYSQL_USER=root
    export MYSQL_PASSWORD=campusos
fi

exec java -XX:+UseContainerSupport -XX:MaxRAMPercentage=60.0 -jar app.jar
