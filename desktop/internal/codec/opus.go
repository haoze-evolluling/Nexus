//go:build steamvoice_opus && cgo

package codec

/*
#cgo windows LDFLAGS: -Wl,-Bstatic -lopus -Wl,-Bdynamic
*/
import "C"

import (
	"encoding/binary"
	"fmt"
	"sync"

	"github.com/hraban/opus"
)

const (
	SampleRate     = 48000
	Channels       = 2
	FrameSamples10 = 480
	MinBitrate     = 48000
	MaxBitrate     = 192000
	BitrateStep    = 16000
)

type OpusEncoder struct {
	encoder      *opus.Encoder
	bitrate      int
	frameSamples int
	mu           sync.Mutex
}

func NewOpusEncoder(bitrate int, frameMs int) (*OpusEncoder, error) {
	if frameMs != 10 && frameMs != 20 {
		return nil, fmt.Errorf("unsupported frame duration: %d ms", frameMs)
	}
	bitrate = ClampBitrate(bitrate)
	e, err := opus.NewEncoder(SampleRate, Channels, opus.AppAudio)
	if err != nil {
		return nil, err
	}
	for _, setting := range []func() error{func() error { return e.SetBitrate(bitrate) }, func() error { return e.SetInBandFEC(true) }, func() error { return e.SetDTX(true) }, func() error { return e.SetPacketLossPerc(5) }} {
		if err := setting(); err != nil {
			return nil, fmt.Errorf("configure opus: %w", err)
		}
	}
	return &OpusEncoder{encoder: e, bitrate: bitrate, frameSamples: FrameSamples10 * frameMs / 10}, nil
}

func ClampBitrate(b int) int {
	if b < MinBitrate {
		return MinBitrate
	}
	if b > MaxBitrate {
		return MaxBitrate
	}
	return MinBitrate + ((b-MinBitrate+BitrateStep/2)/BitrateStep)*BitrateStep
}

func (e *OpusEncoder) SetBitrate(b int) error {
	e.mu.Lock()
	defer e.mu.Unlock()
	b = ClampBitrate(b)
	if err := e.encoder.SetBitrate(b); err != nil {
		return err
	}
	e.bitrate = b
	return nil
}

func (e *OpusEncoder) Bitrate() int { e.mu.Lock(); defer e.mu.Unlock(); return e.bitrate }

func (e *OpusEncoder) EncodePCM(frame []byte) ([]byte, error) {
	e.mu.Lock()
	defer e.mu.Unlock()
	if len(frame) != e.frameSamples*Channels*2 {
		return nil, fmt.Errorf("opus frame must be %d bytes, got %d", e.frameSamples*Channels*2, len(frame))
	}
	pcm := make([]int16, e.frameSamples*Channels)
	for i := range pcm {
		pcm[i] = int16(binary.LittleEndian.Uint16(frame[i*2:]))
	}
	out := make([]byte, 1500)
	n, err := e.encoder.Encode(pcm, out)
	if err != nil {
		return nil, err
	}
	return append([]byte(nil), out[:n]...), nil
}
