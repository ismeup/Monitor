# Monitor

This is an utility, which provides system information about your server's health

## Compiling
The best way to compile executable jar-file is Maven:

    git clone https://github.com/ismeup/Monitor.git
    cd Monitor
    mvn assembly:assembly

## Configuring
To run this utility you need to create file, named config.json and set AES-key, bind port, bind address and mount points, if you need them

    {
      "key" : "<YOUR_AES_KEY>",
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
