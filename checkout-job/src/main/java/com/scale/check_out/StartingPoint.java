package com.scale.check_out;

public class StartingPoint {

    public static void main(String[] args) {
        try {
            String protocol = System.getenv().getOrDefault("PROTOCOL", "GRPC");
            int paramPort = args.length > 0 ? Integer.parseInt(args[0]) : 7000;

            if ("REST".equalsIgnoreCase(protocol)) {
                CheckOutApp.setupREST()
                        .startOnPort(paramPort);
            } else {
                CheckOutApp.setupGRPC()
                        .startOnPort(paramPort);
            }
        } catch (Exception e) {
            e.printStackTrace(System.err);
            System.exit(2);
        }
    }

}
