package main

import (
	"context"
	"fmt"
	"log"
	"net"

	pb "github.com/mcnaveen/health-connect-webhook/server/example/go/gen/hcwebhook/v1"
	"google.golang.org/grpc"
)

const port = 50051

type server struct {
	pb.UnimplementedHealthWebhookServer
}

func (s *server) Deliver(ctx context.Context, req *pb.HealthPayload) (*pb.DeliverResponse, error) {
	log.Printf(
		"Deliver timestamp=%s app_version=%s steps=%d heart_rate=%d sleep=%d",
		req.GetTimestamp(),
		req.GetAppVersion(),
		len(req.GetSteps()),
		len(req.GetHeartRate()),
		len(req.GetSleep()),
	)
	return &pb.DeliverResponse{Ok: true, Message: "accepted"}, nil
}

func main() {
	lis, err := net.Listen("tcp", fmt.Sprintf(":%d", port))
	if err != nil {
		log.Fatalf("listen: %v", err)
	}
	s := grpc.NewServer()
	pb.RegisterHealthWebhookServer(s, &server{})
	log.Printf("HC Webhook gRPC example listening on :%d", port)
	log.Printf("App target: http://YOUR_LAN_IP:%d", port)
	if err := s.Serve(lis); err != nil {
		log.Fatalf("serve: %v", err)
	}
}
