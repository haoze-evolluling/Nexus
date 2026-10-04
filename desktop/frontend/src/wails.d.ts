declare module '../wailsjs/go/main/App' {
  export function DiscoverDevices(): Promise<void>
  export function Connect(device: {name:string;host:string;port:number;id:string;bitrate:number;frameMs:number}): Promise<void>
  export function Disconnect(deviceID: string): Promise<void>
  export function GetStatus(): Promise<{connectedCount:number;message:string;devices:{deviceId:string;name:string;connected:boolean;message:string;bitrate:number;frameMs:number}[]}>
}
declare module '../wailsjs/runtime/runtime' { export function EventsOn(name:string, callback:(value:any)=>void): void }
