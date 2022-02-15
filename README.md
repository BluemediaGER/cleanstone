# Cleanstone

[![Codacy Badge](https://app.codacy.com/project/badge/Grade/344a87fb7df7408f991b574f147d9636)](https://www.codacy.com/manual/BluemediaGER/cleanstone?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=BluemediaGER/cleanstone&amp;utm_campaign=Badge_Grade)  

Cleanstone is a fast and easy to use reverse proxy for Minecraft.  
It can be used to make multiple Minecraft servers available on the same port and IP address. Based on the domain name sent by the client in the handshake, Cleanstone decides which of the available backend servers will receive the client's traffic.  
The subsequent traffic is then transparently forwarded by Cleanstone to the corresponding backend server without the player noticing.

## Compiling

This project uses Maven as its build system. To compile a jar file including all dependencies, you can run the following command:
```bash
mvn clean package
```

## Configuration

The configuration of Cleanstone is very simple. It is a single configuration file in the widely used JSON format.  
The location of the configuration file can be determined in several ways:

- Set the environment variable CLEANSTONE_CONFIG to the path of the folder containing the config.json file for Cleanstone
- If the environment variable CLEANSTONE_CONFIG is not set, Cleanstone will attempt to load the config.json from the folder containing its executable JAR file

An example of a possible configuration file is shown below:

```json
{
  "listenPort": 25565,
  "backendServerMappings": [
      {
        "mappingDomain": "mc.example.com",
        "backendServerAddress": "192.168.1.10",
        "backendServerPort": 25565
      },
      {
        "mappingDomain": "ftb.example.com",
        "backendServerAddress": "192.168.1.20",
        "backendServerPort": 25565
      }
  ]
}
```

Explanation of the configuration keys:

| Key                  | Description                                                                                                                         |
|----------------------|-------------------------------------------------------------------------------------------------------------------------------------|
| listenPort           | Port under which Cleanstone listens for incoming client connections.                                                                |
| mappingDomain        | Domain name, which the client sends in the handshake. This is used to decide to which backend server the data traffic is forwarded. |
| backendServerAddress | Address (hostname or IP address) under which the backend server for the given domain name is available.                             |
| backendServerPort    | Port on which the backend server for the given domain name is listening for connections.                                            |

You can theoretically configure as many backend servers as you like.  
How many players and active connections Cleanstone can handle however depends on the performance of your computer or server.

### PROXY protovol v2 support

Cleanstone supports the PROXY protocol v2, which allows sending the real IP address of a player to the backend server. Cleanstone can either provide the necessary information itself or receive and process it from an upstream proxy such as Cloudflare. The configuration is described below.

```json
{
  "listenPort": 25565,
  "proxyProtocol": {
    "enable": true,
    "passThrough": true
  },
  "backendServerMappings": [
      "..."
  ]
}
```

Explanation of the PROXY protocol v2 configuration keys:

| Key                  | Description                                                                                                                         |
|----------------------|-------------------------------------------------------------------------------------------------------------------------------------|
| enable               | Enable PROXY protocol v2 support. If true cleanstone will process and add an PROXY protocol header for new connections.             |
| passThrough          | (Optional) Process PROXY protocol headers from an upstream proxy (e.g. Cloudflare) and pass the original header to the backend.     |

## Built With  
  
- [Maven](https://maven.apache.org/) - Dependency Management 
- [Jackson-Databind](https://github.com/FasterXML/jackson-databind) - FasterXML Jackson object mapper
- [LOGBack](http://logback.qos.ch/) - Java Logging Framework

## License  
  
This project is licensed under the GNU General Public License v3.0 - see the [LICENSE](LICENSE) file for details