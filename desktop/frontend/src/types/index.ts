export type Theme = 'light' | 'dark' | 'system';
export type Locale = 'zh' | 'en';
export type LanguagePref = 'system' | 'zh' | 'en';
export type ParamValue = string | number;
export type ChannelRoute = 'stereo' | 'left' | 'right';

export interface Settings {
  bitrate: number;
  frameMs: number;
  theme: Theme;
  updatedAtMs: number;
  deviceId: string;
}

export interface Device {
  name: string;
  host: string;
  port: number;
  id: string;
  codec: string;
  sampleRate: number;
  channels: number;
  bitrate: number;
  frameMs: number;
  supportedFrameMs: number[];
  updatedAtMs: number;
  settingsDeviceId: string;
}

export interface DeviceStatus {
  deviceId: string;
  name: string;
  connected: boolean;
  message: string;
  bitrate: number;
  frameMs: number;
  phase: number;
  channelRoute: ChannelRoute;
}

export interface Calibration {
  phase: number;
  offsetMs: number;
  rttMs: number;
  updatedAt: number;
}

export interface ConnRequest {
  requestId: string;
  deviceId: string;
  name: string;
  host: string;
}

export interface AuthorizedDevice {
  ID: string;
  Name: string;
  AddedAtMs: number;
}

export interface Identity {
  deviceId: string;
  name: string;
}

export interface NTPStatus {
  server?: string;
  reachable?: boolean;
  offsetMs?: number;
}

export type StatusInfo = {
  kind: 'key' | 'backend';
  value: string;
  params?: Record<string, ParamValue>;
} | null;
