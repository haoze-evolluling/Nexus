//go:build !steamvoice_opus || !cgo

package codec

import "errors"

const (
	SampleRate     = 48000
	Channels       = 2
	FrameSamples10 = 480
	MinBitrate     = 48000
	MaxBitrate     = 192000
	BitrateStep    = 16000
)

type OpusEncoder struct{}

func NewOpusEncoder(int, int) (*OpusEncoder, error) {
	return nil, errors.New("Opus encoder unavailable: build with -tags steamvoice_opus and install libopus")
}
func (e *OpusEncoder) SetBitrate(int) error { return errors.New("Opus encoder unavailable") }
func (e *OpusEncoder) EncodePCM([]byte) ([]byte, error) {
	return nil, errors.New("Opus encoder unavailable")
}
