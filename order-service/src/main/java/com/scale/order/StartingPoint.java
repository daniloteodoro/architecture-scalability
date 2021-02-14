package com.scale.order;

public class StartingPoint {

    public static void main(String[] args) {
        try {
            String protocol = System.getenv().getOrDefault("PROTOCOL", "REACTIVE_REST");
            int paramPort = args.length > 0 ? Integer.parseInt(args[0]) : 8000;

            if ("REACTIVE_REST".equalsIgnoreCase(protocol)) {
                OrderAppUsingReactiveREST.defaultSetup()
                        .startOnPort(paramPort);
            } else if ("REST".equalsIgnoreCase(protocol)) {
                OrderAppUsingREST.defaultSetup()
                        .startOnPort(paramPort);
            } else {
                OrderAppUsingGRPC.defaultSetup()
                        .startOnPort(paramPort, true);
            }
        } catch (Exception e) {
            e.printStackTrace(System.err);
            System.exit(1);
        }
    }

}
