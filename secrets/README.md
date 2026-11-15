# Docker secrets

The `*.txt` files of this folder hold the database credentials and are ignored by git.
Create them before the first `docker compose up`, without trailing newline:

```bash
printf 'gutendaar' > secrets/db_name.txt
printf 'daar'      > secrets/db_user.txt
openssl rand -base64 24 | tr -d '\n' > secrets/db_password.txt
```

PostgreSQL only reads them when its `pgdata` volume is created: to change them afterwards,
either run `ALTER USER` in the database, or delete the volume (`docker compose down -v`, data lost).
