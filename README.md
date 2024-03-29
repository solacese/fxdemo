# FX DEMO README

## What does this demonstrate?
Demonstrating live forex rates dashboard using Solace PubSub+ Event Broker with MQTT over Websockets.

**Try the live demo in action here:
https://sg.solace.com/fx/**

### Other Useful Links
List of links to useful resources ...


## Contents

app folder contains the Java program to generate randomized forex rates
web folder contains the dashboard web page


## Checking out

To check out the project, clone this GitHub repository:

```
git clone https://github.com/solacese/github-demo
cd <github-demo>
```

## Running the Demo

First, open the forex rates web page at https://sg.solace.com/fx/.
Then, run the forex rate generator Java application as per below examples with Docker or locally. 

```
$ docker run --name fxdemo1.0 --env HOST=hostname:55555 --env VPN=default --env USER=default --env PASSWORD=default --env ROOT_TOPIC=fxrates --env SYMBOLS=config/symbols.properties --env FREQUENCY=1 arih1299/fxdemo-app:1.0
$ java -jar app/fxdemo-1.0.jar -h sgdemo1.solace.com -v FXstream -u <username> -p <password> -i app/config/symbols.properties -t fxrates
```

Play around with currency watch list which translates to individual subscriptions. This can be shown with the Console log and client subscriptions from Solace web UI.
![sample demo](https://github.com/solacese/fxdemo/blob/master/howtodemo.png "How to demo")

## Contributing

Please read [CONTRIBUTING.md](CONTRIBUTING.md) for details on our code of conduct, and the process for submitting pull requests to us.

## Authors

See the list of [contributors](https://github.com/solacese/<github-repo>/graphs/contributors) who participated in this project.

## License

This project is licensed under the Apache License, Version 2.0. - See the [LICENSE](LICENSE) file for details.

## Resources

For more information try these resources:

- The Solace Developer Portal website at: http://dev.solace.com
- Get a better understanding of [Solace technology](http://dev.solace.com/tech/).
- Check out the [Solace blog](http://dev.solace.com/blog/) for other interesting discussions around Solace technology
- Ask the [Solace community.](http://dev.solace.com/community/)

