export namespace config {
	
	export class AuthorizedDevice {
	    id: string;
	    name: string;
	    addedAtMs: number;
	
	    static createFrom(source: any = {}) {
	        return new AuthorizedDevice(source);
	    }
	
	    constructor(source: any = {}) {
	        if ('string' === typeof source) source = JSON.parse(source);
	        this.id = source["id"];
	        this.name = source["name"];
	        this.addedAtMs = source["addedAtMs"];
	    }
	}

}

export namespace main {
	
	export class Device {
	    Name: string;
	    Host: string;
	    Port: number;
	    ID: string;
	    Codec: string;
	    SampleRate: number;
	    Channels: number;
	    Bitrate: number;
	    FrameMs: number;
	    SupportedFrameMs: number[];
	    UpdatedAtMs: number;
	    SettingsDeviceID: string;
	
	    static createFrom(source: any = {}) {
	        return new Device(source);
	    }
	
	    constructor(source: any = {}) {
	        if ('string' === typeof source) source = JSON.parse(source);
	        this.Name = source["Name"];
	        this.Host = source["Host"];
	        this.Port = source["Port"];
	        this.ID = source["ID"];
	        this.Codec = source["Codec"];
	        this.SampleRate = source["SampleRate"];
	        this.Channels = source["Channels"];
	        this.Bitrate = source["Bitrate"];
	        this.FrameMs = source["FrameMs"];
	        this.SupportedFrameMs = source["SupportedFrameMs"];
	        this.UpdatedAtMs = source["UpdatedAtMs"];
	        this.SettingsDeviceID = source["SettingsDeviceID"];
	    }
	}
	export class DeviceStatus {
	    DeviceID: string;
	    Name: string;
	    Connected: boolean;
	    Message: string;
	    Bitrate: number;
	    FrameMs: number;
	    Phase: number;
	    ChannelRoute: string;
	
	    static createFrom(source: any = {}) {
	        return new DeviceStatus(source);
	    }
	
	    constructor(source: any = {}) {
	        if ('string' === typeof source) source = JSON.parse(source);
	        this.DeviceID = source["DeviceID"];
	        this.Name = source["Name"];
	        this.Connected = source["Connected"];
	        this.Message = source["Message"];
	        this.Bitrate = source["Bitrate"];
	        this.FrameMs = source["FrameMs"];
	        this.Phase = source["Phase"];
	        this.ChannelRoute = source["ChannelRoute"];
	    }
	}
	export class Identity {
	    DeviceID: string;
	    Name: string;
	
	    static createFrom(source: any = {}) {
	        return new Identity(source);
	    }
	
	    constructor(source: any = {}) {
	        if ('string' === typeof source) source = JSON.parse(source);
	        this.DeviceID = source["DeviceID"];
	        this.Name = source["Name"];
	    }
	}
	export class NTPStatus {
	    Server: string;
	    OffsetMs: number;
	    Reachable: boolean;
	
	    static createFrom(source: any = {}) {
	        return new NTPStatus(source);
	    }
	
	    constructor(source: any = {}) {
	        if ('string' === typeof source) source = JSON.parse(source);
	        this.Server = source["Server"];
	        this.OffsetMs = source["OffsetMs"];
	        this.Reachable = source["Reachable"];
	    }
	}
	export class Status {
	    ConnectedCount: number;
	    Message: string;
	    Devices: DeviceStatus[];
	
	    static createFrom(source: any = {}) {
	        return new Status(source);
	    }
	
	    constructor(source: any = {}) {
	        if ('string' === typeof source) source = JSON.parse(source);
	        this.ConnectedCount = source["ConnectedCount"];
	        this.Message = source["Message"];
	        this.Devices = this.convertValues(source["Devices"], DeviceStatus);
	    }
	
		convertValues(a: any, classs: any, asMap: boolean = false): any {
		    if (!a) {
		        return a;
		    }
		    if (a.slice && a.map) {
		        return (a as any[]).map(elem => this.convertValues(elem, classs));
		    } else if ("object" === typeof a) {
		        if (asMap) {
		            for (const key of Object.keys(a)) {
		                a[key] = new classs(a[key]);
		            }
		            return a;
		        }
		        return new classs(a);
		    }
		    return a;
		}
	}

}

