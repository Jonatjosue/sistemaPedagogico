FROM mariadb:11.4
COPY 01-schema.sql /docker-entrypoint-initdb.d/01-schema.sql
CMD ["mariadbd", \
     "--datadir=/var/lib/mysql/data", \
     "--bind-address=::", \
     "--character-set-server=utf8mb4", \
     "--collation-server=utf8mb4_unicode_ci", \
     "--default-time-zone=-06:00", \
     "--innodb-buffer-pool-size=64M", \
     "--performance-schema=OFF", \
     "--max-connections=40"]