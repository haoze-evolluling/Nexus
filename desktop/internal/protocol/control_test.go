package protocol

import "testing"

func TestFeedbackRoundTripCarriesCalibration(t *testing.T) {
	encoded := EncodeFeedback(ReceiverFeedback{Session: 7, HighestSeq: 12, Received: 10, Lost: 2, Queue: 1, Bitrate: 96000, SyncState: SyncPlaying, OffsetMs: -3, RttMs: 9})
	if len(encoded) != FeedbackSize {
		t.Fatalf("encoded feedback length = %d, want %d", len(encoded), FeedbackSize)
	}
	decoded, err := DecodeFeedback(encoded)
	if err != nil {
		t.Fatalf("decode failed: %v", err)
	}
	if decoded.SyncState != SyncPlaying || decoded.OffsetMs != -3 || decoded.RttMs != 9 {
		t.Fatalf("calibration fields lost: %+v", decoded)
	}
	if decoded.Session != 7 || decoded.Lost != 2 || decoded.Queue != 1 || decoded.Bitrate != 96000 {
		t.Fatalf("feedback fields lost: %+v", decoded)
	}
}

func TestDecodeFeedbackRejectsBadLength(t *testing.T) {
	if _, err := DecodeFeedback(make([]byte, 31)); err == nil {
		t.Fatal("expected invalid feedback of length 31 to be rejected")
	}
}
