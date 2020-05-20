---
title: Gatekeeper Configuration
---

# Germinate Configuration
This page describes the content of Gatekeeper's configuration folder. This folder does not exist and has to be created somewhere on the system where Gatekeeper can access it.
The folder only has to contain a single file: `config.properties`.

The content of this file is explained below. This includes properties like the database details and user credentials as well as various customization options.

```ini
# The public-facing URL of Gatekeeper. This is required so that the server can generate files that link back to the user interface
web.base=<base url of the client, e.g. https://ics.hutton.ac.uk/germinate-gatekeeper/>

# Salts are used to slow down brute-force attacks by making each individual authentication check slower.
salt=<the "cost-factor" of the password check, larger values make authentication slower. default: 10>

# Email properties. address, username and server are required.    
email.address=<email address to use when sending emails>
email.username=<email address username>
email.password=<email address password>
email.server=<email server url>
email.port=<email server port>

# Database properties. Server name, database name and user name are required.
# A password may be optional depending on your configuration and the port only needs to be provided if it's not 3306. 
database.server   = <database server>
database.name     = <database name>
database.username = <database username>
database.password = <database password if required
database.port     = <database port if not 3306>
```

# Default Admin account

When you run Gatekeeper for the first time, a default admin account is created. The default password for this account is simply "password". Please change this immediately by logging in using the username "admin" and the aforementioned password.