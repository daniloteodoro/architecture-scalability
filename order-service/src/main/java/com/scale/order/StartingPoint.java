package com.scale.order;

public class StartingPoint {

    public static void main(String[] args) {
        try {
            String protocol = System.getenv().getOrDefault("PROTOCOL", "GRPC");
            int paramPort = args.length > 0 ? Integer.parseInt(args[0]) : 8000;

            if ("REST".equalsIgnoreCase(protocol)) {
                OrderAppUsingREST.defaultSetup()
                        .startOnPort(paramPort);
            } else {
                OrderAppUsingGRPC.defaultSetup()
                        .startOnPort(paramPort, true);
            }
        } catch (Exception e) {
            e.printStackTrace(System.err);
            System.exit(-1);
        }
    }

}
