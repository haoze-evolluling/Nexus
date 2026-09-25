//go:build steamvoice_opus && cgo

package codec

import (
	"testing"
)

func TestOpusEncoderEncode(t *testing.T) {
	encoder, err := NewOpusEncoder(128000, 10)
	if err != nil {
		t.Fatalf("NewOpusEncoder failed: %v", err)
	}

	if encoder.Bitrate() != 128000 {
		t.Fatalf("expected bitrate 128000, got %d", encoder.Bitrate())
	}

	// 10 ms at 48000 Hz stereo = 480 samples * 2 channels * 2 bytes = 1920 bytes
	pcm := make([]byte, 1920)
	encoded, err := encoder.EncodePCM(pcm)
	if err != nil {
		t.Fatalf("EncodePCM failed: %v", err)
	}
	if len(encoded) == 0 {
		t.Fatalf("expected non-empty encoded frame")
	}

	if err := encoder.SetBitrate(96000); err != nil {
		t.Fatalf("SetBitrate failed: %v", err)
	}
	if encoder.Bitrate() != 96000 {
		t.Fatalf("expected bitrate 96000, got %d", encoder.Bitrate())
	}
}

func TestOpusEncoderInvalidFrame(t *testing.T) {
	_, err := NewOpusEncoder(128000, 15)
	if err == nil {
		t.Fatalf("expected error for invalid frame duration")
	}

	encoder, err := NewOpusEncoder(128000, 20)
	if err != nil {
		t.Fatalf("NewOpusEncoder failed: %v", err)
	}

	// 20 ms expects 3840 bytes, give 100 bytes
	_, err = encoder.EncodePCM(make([]byte, 100))
	if err == nil {
		t.Fatalf("expected error for mismatched frame size")
	}
}
