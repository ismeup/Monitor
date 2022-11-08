# Monitor

This is an utility, which provides information about your server's health. 
To use it, you need to register new Monitor in your client area and provide config.json file
(look for Configuring and running section).


## Compiling
The best way to compile executable jar-file is Maven:

    git clone https://github.com/ismeup/Monitor.git
    cd Monitor
    mvn assembly:assembly

## Configuring and running

### Preferred method (since 1.2)
The best method, to configure your monitor, is passing --setup argument to your monitor:

    java -jar Monitor.jar --setup

It will ask you for login, password and some other questions, like bind port, 
AES encryption key and others.
After that, it will register your Monitor in your account and generate config.json file
Then, you will be able to run Monitor as usual:

    java -jar Monitor.jar


### Manual configuration

In manual mode, you need to register your Monitor in your Client Area. 
You can do this by next steps:

1) Open server, on which you want to configure system resource checks (add it, if server is not created)
2) Open "Monitors" tab and create a new one by clicking "+" button
3) Provide next information:
    * Name - it will be displayed in your client area
    * Host - IP or domain name to connect to (where we can find your Monitor)
    * Port - on which port your Monitor will be available (must be same with **port** in config.json)
    * AES-Key - connection will be encrypted by this passphrase  (must be same with **key** in config.json)

Next, you need to create file, named **config.json** and provide AES-key, bind port, bind address and mount points, if you need them:

#### config.json sample:
    {
      "key" : "<HERE YOU MUST CREATE AND PUT YOUR AES PASSPHRASE. IT MUST BE SAME WITH AES KEY IN YOUR MONITOR SETTINGS IN CLIENT AREA>", 
      "port" : 5555,
      "bind" : "0.0.0.0",
      "mount_points" : [
          {
              "name" : "root",
              "path" : "/"
          },
          {
              "name" : "home",
              "path" : "/home"
          }
      ]
    } 
