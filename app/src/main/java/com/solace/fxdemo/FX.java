package com.solace.fxdemo;

import com.google.common.base.Splitter;
import com.solacesystems.jcsmp.*;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class FX {

    private static final Logger log = LogManager.getLogger(FX.class);

    private static double FREQUENCY_IN_SECONDS = 1;

    private static String SOLACE_IP_PORT = null;
    private static String SOLACE_VPN = null;
    private static String SOLACE_CLIENT_USERNAME = null;
    private static String SOLACE_PASSWORD = null;
    private static String ROOT_TOPIC = null;
    private static String SYMBOLS = null;

    private static Properties symbolsList = null;

    /**
     * @param args
     */
    public static void main(String[] args) {

        FX fxPublisher = new FX();
        if(fxPublisher.parseArgs(args) ==1 || fxPublisher.validateParams() ==1) {
            log.error(fxPublisher.getCommonUsage());
        }
        else {
            FXStreamerThread hwPubThread = fxPublisher.new FXStreamerThread();
            hwPubThread.start();
        }
    }

    private String getCommonUsage() {
        String str = "Common parameters:\n";
        str += "\t -h HOST[:PORT]  Router IP address [:port, omit for default]\n";
        str += "\t -v VPN          vpn name (omit for default)\n";
        str += "\t -u USER         Authentication username\n";
        str += "\t -t ROOT TOPIC   Root topic name\n";
        str += "\t -i SYMBOLS      Properties file containing symbols\n";
        str += "\t[-p PASSWORD]    Authentication password\n";
        str += "\t[-f FREQUENCY]   Frequency of publish in seconds (default: 5)\n";
        return str;
    }

    private int parseArgs(String[] args) {
        try {
            for (int i = 0; i < args.length; i++) {
                if (args[i].equals("-h")) {
                    i++;
                    SOLACE_IP_PORT = args[i];
                } else if (args[i].equals("-u")) {
                    i++;
                    SOLACE_CLIENT_USERNAME = args[i];
                } else if (args[i].equals("-p")) {
                    i++;
                    SOLACE_PASSWORD = args[i];
                } else if (args[i].equals("-i")) {
                    i++;
                    SYMBOLS = args[i];
                } else if (args[i].equals("-v")) {
                    i++;
                    SOLACE_VPN = args[i];
                } else if (args[i].equals("-t")) {
                    i++;
                    ROOT_TOPIC = args[i];
                } else if (args[i].equals("-f")) {
                    i++;
                    try {
                        FREQUENCY_IN_SECONDS = Double.parseDouble(args[i]);
                    }
                    catch (NumberFormatException nfe) {
                        return 1; // err: print help
                    }
                } else if (args[i].equals("--help")) {
                    return 1; // err: print help
                } else {
                    return 1; // err: print help
                }

            }
        } catch (Exception e) {
            return 1; // err
        }

        return 0; // success
    }

    private int validateParams(){
        if (SOLACE_IP_PORT == null) return 1;
        if (SOLACE_VPN == null) SOLACE_VPN = "default";
        if (SOLACE_CLIENT_USERNAME == null) return 1;
        if (SYMBOLS == null) return 1;
        return 0;
    }

    /**
     * Thread class to generate pixels to move every "n" seconds, where "n" is the Frequency In Seconds passed
     * to the thread upon instantiation
     */
    class FXStreamerThread extends Thread {

        JCSMPSession session = null;
        XMLMessageProducer prod = null;

        public void run() {

            while (true){

                try {

                    initSymbolsList();
                    initSolace();

                    Random random = new Random();

                    double price, change, buySellSpreadPct, buy, sell = 0.0d;
                    String directionString = "";
                    int directionInt = 0;

                    @SuppressWarnings("unchecked")
                    Enumeration<String> enumSymbols = (Enumeration<String>) symbolsList.propertyNames();

                    while (enumSymbols.hasMoreElements())
                    {
                        StringBuffer payload = new StringBuffer();
                        String msg = null;

                        // (1) Iterate through the symbols list
                        String symbol = enumSymbols.nextElement();

                        // (2) Is this an symbol to get an update this time round?
                        if (random.nextBoolean()) {

                            price = Double.parseDouble( symbolsList.getProperty(symbol) );

                            // (3) Should the price go up or down?
                            directionString = (random.nextBoolean()) ? "+" : "-";
                            directionInt = Integer.parseInt(directionString + "1");

                            // (4) Work out the price change
                            change = directionInt * random.nextDouble();

                            // (5) change the mid price
                            price += change;

                            // calc the spread - random from 0 to 10%
                            buySellSpreadPct = (double) random.nextInt(5) / 100.0;
                            log.info("RAND: " + buySellSpreadPct);

                            // (6) calc buy/sell prices
                            buy = price - (price * buySellSpreadPct);
                            sell = price + (price * buySellSpreadPct);

                            // (7) Create the JSON element from the symbol, price and the up/down direction
                            payload.append(createTradeUpdateElement(symbol, buy, sell, directionString));

                            // This version only apply random changes against a fixed starting price,
                            // it does not demonstrate subsequent price changes yet.
                            // TODO: save the new price back to symbolProps
                            // TODO: once in a while, reset back the price back to original price from file


                            log.debug("topicString="+ ROOT_TOPIC +"/usd/"+symbol.toLowerCase()+"/"+buy+"/"+sell+"\tmessage="+payload);

                            msg = payload.toString().trim();

                            // if not empty, send out
                            if ( ! msg.equalsIgnoreCase("")) {
                                publishToSolace(ROOT_TOPIC +"/usd/"+symbol.toLowerCase()+"/"+buy+"/"+sell, msg);
                            }
                        }
                    }

                    //log.error("Now sleeping for "+(int)(MDDStreamer.FREQUENCY_IN_SECONDS * 1000)+" milliseconds");
                    sleep((int)(FX.FREQUENCY_IN_SECONDS * 1000));

//                } catch (InterruptedException e) {
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        }

        String createTradeUpdateElement(String symbol, double buy, double sell, String directionString) {

            StringBuffer el = new StringBuffer();

            DecimalFormat df_3dec = new DecimalFormat("#.###");

            el.append("{");
            el.append("\"symbol\": \"").append(symbol).append("\", ");
            el.append("\"buying\": ").append(df_3dec.format(buy)).append(", ");
            el.append("\"selling\": ").append(df_3dec.format(sell)).append(", ");
            el.append("\"direction\": \"").append(directionString).append("\"");
            el.append("}");

            return el.toString();
        }

        void publishToSolace(String topicString, String payload) {

            try {
                if (session!=null && session.isClosed()) {
                    log.warn("Session is not ready yet, waiting 5 seconds");
                    Thread.sleep(5000);
                    session.connect();
                    publishToSolace (topicString, payload);
                }
                else if (session == null) {
                    initSolace();
                    publishToSolace (topicString, payload);
                }
                else {

                    Topic topic = JCSMPFactory.onlyInstance().createTopic(topicString);

                    log.info("MESSAGE: " + payload);

                    TextMessage msg = prod.createTextMessage();
                    msg.setText(payload);
                    msg.setDeliveryMode(DeliveryMode.DIRECT);
                    prod.send(msg, topic);
                    Thread.sleep(100);

                    //log.error("Sent message:"+msg.dump());
                }
            } catch (Exception ex) {
                // Normally, we would differentiate the handling of various exceptions, but
                // to keep this sample as simple as possible, we will handle all exceptions
                // in the same way.
                log.error("Encountered an Exception: " + ex.getMessage());
                log.error(ex.getStackTrace());
                finish();
            }
        }

        void initSymbolsList() {

            if (symbolsList != null) return;

            log.info("About to initialise symbols list from file: " + SYMBOLS);

            try (InputStream input = new FileInputStream(SYMBOLS)) {

                // This version will be updated with new prices
                symbolsList = new Properties();
                symbolsList.load(input);

                log.info("Loaded " + symbolsList.size() + " symbols from file.");

            } catch (IOException ex) {
                log.error("Encountered an Exception: " + ex.getMessage());
                log.error(ex.getStackTrace());
                finish();
            }
        }

        void initSolace() {
            log.debug("Initializing...");

            if (session!=null && !session.isClosed()) return;

            try {
                log.info("About to create session.");

                JCSMPProperties properties = new JCSMPProperties();

                properties.setProperty(JCSMPProperties.HOST, SOLACE_IP_PORT);
                properties.setProperty(JCSMPProperties.USERNAME, SOLACE_CLIENT_USERNAME);
                properties.setProperty(JCSMPProperties.VPN_NAME, SOLACE_VPN);
                properties.setProperty(JCSMPProperties.PASSWORD, SOLACE_PASSWORD);
                properties.setBooleanProperty(JCSMPProperties.REAPPLY_SUBSCRIPTIONS, true);

                // Channel properties
                JCSMPChannelProperties chProperties = (JCSMPChannelProperties) properties
                        .getProperty(JCSMPProperties.CLIENT_CHANNEL_PROPERTIES);

                chProperties.setConnectRetries(10);
                chProperties.setConnectTimeoutInMillis(1000);
                chProperties.setReconnectRetries(2);
                chProperties.setReconnectRetryWaitInMillis(3000);

                session =  JCSMPFactory.onlyInstance().createSession(properties, null, new PrintingSessionEventHandler());
                session.connect();

                // Acquire a message producer.
                prod = session.getMessageProducer(new PrintingPubCallback());
                log.info("Session:"+session.getSessionName());
                log.info("Acquired message producer:"+prod);

            } catch (Exception ex) {
                log.error("Encountered an Exception: " + ex.getMessage());
                log.error(ex.getStackTrace());
                finish();
            }
        }

        void finish() {

            if (session != null) {
                session.closeSession();
            }
            System.exit(1);
        }


        private Map<String, String> parseMap(String formattedMap) {
            return Splitter.on(",").withKeyValueSeparator("=").split(formattedMap);
        }
    }

    public class PrintingSessionEventHandler implements SessionEventHandler {
        public void handleEvent(SessionEventArgs event) {
            log.warn("Received Session Event "+event.getEvent()+ " with info "+event.getInfo());
        }
    }

    public class PrintingPubCallback implements JCSMPStreamingPublishEventHandler {
        public void handleError(String messageID, JCSMPException cause, long timestamp) {
            log.error("Error occurred for message: " + messageID);
            cause.printStackTrace();
        }

        public void responseReceived(String messageID) {
            log.info("Response received for message: " + messageID);
        }
    }

}

