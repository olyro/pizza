/* tslint:disable */
// Generated using typescript-generator version 2.8.449 on 2020-01-01 23:44:30.

export interface GetOrder {
    id: string;
}

export interface SendFax {
    name: string;
    phoneNumber: string;
}

export interface SendFaxSipgateRequest {
    faxlineId: string;
    recipient: string;
    filename: string;
    base64Content: string;
}

export interface SendFaxSipgateResponse {
    sessionId: string;
}

export interface SetHidden {
    id: string;
    hidden: boolean;
}

export interface SetPayed {
    id: string;
    payed: boolean;
}

export interface StatusFaxSipgateResponse {
    faxStatusType: string;
}

export interface FaxMessage extends Message {
    status: FaxStatus;
}

export interface Item {
    id: string;
    name: string;
    sizes: Size[];
}

export interface MenuItem {
    item: Item;
    description: string;
    extras: Item[];
}

export interface Message {
    kind: string;
}

export interface Order {
    id: string;
    name: string;
    payed: boolean;
    items: OrderItem[];
}

export interface OrderItem {
    id: string;
    size: string;
    extraIds: string[];
}

export interface OrderMessage extends Message {
    orders: Order[];
}

export interface Size {
    name: string;
    price: number;
}

export type FaxStatus = "SENT" | "FAILED" | "PENDING" | "NOFAX" | "TIMEOUT";

export type UserRole = "ANYONE" | "ADMIN";
