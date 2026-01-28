$postgreDataPath = "/var/lib/docker-postgres/migration-stack"

if ($args[0] -eq "stop") {
    docker stop migration-postgre *> $null
} elseif ($args[0] -eq "rm") {
    docker stop migration-postgre *> $null
    docker rm migration-postgre *> $null
} else {
    $status = docker ps -a -f name=migration-postgre --format "{{.Status}}" | ForEach-Object { $_.split(" ")[0]}

    if ($status -eq "Exited") {
        docker start migration-postgre *> $null
    } else {
        docker run -d `
            --name migration-postgre `
            -p 5432:5432 `
            -e POSTGRES_USER=migrationadmin `
            -e POSTGRES_PASSWORD="password" `
            -e POSTGRES_DB="migrationdb" `
            -e PGDATA=/var/lib/postgresql/data/pgdata `
            -v ${postgreDataPath}:/var/lib/postgresql/data `
            postgres
    }
}

