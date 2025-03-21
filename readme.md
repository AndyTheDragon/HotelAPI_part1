Remember to add a config.properties file in the resources directory. The config.properties file should contain the following properties:
```
DB_NAME=
DB_USERNAME=postgres
DB_PASSWORD=
SECRET_KEY=minimum32characterslong
ISSUER=
TOKEN_EXPIRE_TIME=3600000
```
The DB_NAME, DB_USERNAME, DB_PASSWORD, ISSUER, and TOKEN_EXPIRE_TIME properties should be filled in with the appropriate values. The SECRET_KEY property should be a minimum of 32 characters long.
