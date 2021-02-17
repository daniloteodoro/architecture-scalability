package com.scale.check_out;

public class StartingPoint {

    public static void main(String[] args) {
        try {
            String protocol = System.getenv().getOrDefault("PROTOCOL", "REST");
            int paramPort = args.length > 0 ? Integer.parseInt(args[0]) : 7000;

            if ("REACTIVE_REST".equalsIgnoreCase(protocol)) {
                ReactiveCheckOutApp.setupReactiveREST()
                        .startOnPort(paramPort);
            } else if ("REST".equalsIgnoreCase(protocol)) {
                CheckOutApp.setupREST()
                        .startOnPort(paramPort);
            } else {
                CheckOutApp.setupGRPC()
                        .startOnPort(paramPort);
            }
        } catch (Exception e) {
            e.printStackTrace(System.err);
            System.exit(1);
        }
    }

}
