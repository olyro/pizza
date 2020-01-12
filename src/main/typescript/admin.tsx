import * as React from "react";
import { Order, MenuItem, Message, OrderMessage, SetPayed, GetOrder, SendFax, SetHidden, FaxStatus, FaxMessage } from "../../../generated_types";
import { renderPrice } from "./index";

interface AdminInterfaceProps {
    secretKey: string
}

interface AdminInterfaceState {
    orders: Array<Order>
    menuItems: Array<MenuItem>
    name: string
    phone: string
    address: string
    faxStatus: FaxStatus
}

export let calcPrice = (order: Order, menuItems: Array<MenuItem>): number => {
    if (menuItems.length === 0) return 0;
    return order.items.map(oi => {
        let menuItem = menuItems.find(mi => mi.item.id === oi.id)!;
        let basePrice = menuItem.item.sizes.find(s => s.name === oi.size)!.price;
        let extraPrices = oi.extraIds.map(eid => menuItem.extras.find(e => e.id === eid)!.sizes.find(s => s.name === oi.size)!.price).reduce((pv, cv) => pv + cv, 0);
        return basePrice + extraPrices;
    }).reduce((pv, cv) => pv + cv, 0);
}


export class AdminInterface extends React.Component<AdminInterfaceProps, AdminInterfaceState> {
    constructor(props: AdminInterfaceProps) {
        super(props);
        let connect = () => {
            let protocol = location.protocol === "http:" ? "ws:" : "wss:";
            let ws = new WebSocket(`${protocol}//${location.host}/websocket?key=${this.props.secretKey}`);
            ws.onmessage = this.handleMessage;
            ws.onclose = () => {
                setTimeout(() => connect(), 1000);
            };
        }

        connect();

        this.state = {
            orders: [],
            menuItems: [],
            name: "",
            phone: "",
            address: "Theodor-Boveri-Weg\nTreffpunkt 8 Informatik\n97074 Würzburg",
            faxStatus: "NOFAX"
        };

        this.getOrders();
    }

    getOrders = async () => {
        let params = new URLSearchParams();
        params.set("key", this.props.secretKey);
        let menuItems = await (await fetch("/menu")).json()
        this.setState({ menuItems: menuItems, orders: (await (await fetch("/admin/orders?" + params.toString(), { method: "POST" })).json()) });
    };

    handleMessage = (event: any) => {
        let m: Message = JSON.parse(event.data);
        if (m.kind === "OrderMessage") {
            this.setState({ orders: (m as OrderMessage).orders });
        } else if (m.kind === "FaxMessage") {
            this.setState({ faxStatus: (m as FaxMessage).status });
        }
    };

    displayOrder = (order: Order): string => {
        return order.items.map(oi => {
            let menuItem = this.state.menuItems.find(mi => mi.item.id === oi.id)!;
            let baseName = `${menuItem.item.name} (${oi.size})`;
            let extraNames = oi.extraIds.map(eid => "+" + menuItem.extras.find(e => e.id === eid)!.name).join(", ");
            return oi.extraIds.length > 0 ? `${baseName} (${extraNames})` : baseName;
        }).join(", ");
    }

    generateHandleCheck = (id: string) => {
        return async (event: React.ChangeEvent<HTMLInputElement>) => {
            let params = new URLSearchParams();
            params.set("key", this.props.secretKey);
            (await (await fetch("/admin/setPayed?" + params.toString(), { method: "POST", body: JSON.stringify({ id: id, payed: event.target.checked } as SetPayed) })).json())
        };
    };

    generateHandleDelete = (id: string) => {
        return async (event: React.MouseEvent<HTMLAnchorElement, MouseEvent>) => {
            event.preventDefault();
            let params = new URLSearchParams();
            params.set("key", this.props.secretKey);
            (await (await fetch("/admin/deleteOrder?" + params.toString(), { method: "POST", body: JSON.stringify({ id: id } as GetOrder) })).json())
        };
    };

    generateHandleHide = (id: string) => {
        return async (event: React.MouseEvent<HTMLAnchorElement, MouseEvent>) => {
            event.preventDefault();
            let params = new URLSearchParams();
            params.set("key", this.props.secretKey);
            (await (await fetch("/admin/setHidden?" + params.toString(), { method: "POST", body: JSON.stringify({ id: id, hidden: true } as SetHidden) })).json())
        };
    };

    renderOrder = (order: Order) => {
        return (
            <tr className={order.payed ? "payed" : ""}>
                <td><a title={order.id} href={`/myorder/${order.id}`}>{order.name}</a></td>
                <td>{this.displayOrder(order)}</td>
                <td className="text-right">{renderPrice(calcPrice(order, this.state.menuItems))}</td>
                <td><input onChange={this.generateHandleCheck(order.id)} type="checkbox" checked={order.payed}></input></td>
                <td>
                    <a onClick={this.generateHandleDelete(order.id)} href="#">Löschen</a>
                    <span> </span>
                    <a onClick={this.generateHandleHide(order.id)} href="#">Verstecken</a>
                </td>
            </tr>
        );
    }

    sendFax = async (event: React.FormEvent) => {
        event.preventDefault();
        this.setState({ faxStatus: "NOFAX" });
        let params = new URLSearchParams();
        params.set("key", this.props.secretKey);
        let response = fetch("/admin/sendFax?" + params.toString(), { method: "POST", body: JSON.stringify({ name: this.state.name, phoneNumber: this.state.phone, address: this.state.address } as SendFax) })
        if (await ((await response).json())) {
            alert("Bestellung erfolgreich");
        } else {
            alert("Fehler");
        }
    }

    renderFaxStatus = () => {
        switch (this.state.faxStatus) {
            case "FAILED": {
                return (
                    <div className="alert alert-danger" role="alert">Fehlgeschlagen</div>
                );
            }
            case "TIMEOUT": {
                return (
                    <div className="alert alert-danger" role="alert">Timeout</div>
                );
            }
            case "SENT": {
                return (
                    <div className="alert alert-success" role="alert">Erfolgreich versendet</div>
                );
            }
            case "PENDING": {
                return (
                    <div className="alert alert-warning" role="alert">Fax wird versendet ...</div>
                );
            }
        }
    }

    render() {
        return (
            <div className="container">
                <table className="table">
                    <thead>
                        <tr>
                            <th>Name</th>
                            <th>Inhalt</th>
                            <th className="text-right">Preis</th>
                            <th>Bezahlt</th>
                            <th>Aktionen</th>
                        </tr>
                    </thead>
                    <tbody>
                        {this.state.orders.map(order => this.renderOrder(order))}
                    </tbody>
                </table>
                <p><strong>Gesamtpreis: {renderPrice(this.state.orders.map(o => calcPrice(o, this.state.menuItems)).reduce((pv, cv) => pv + cv, 0))}</strong></p>
                <form onSubmit={this.sendFax}>
                    <div className="form-group">
                        <label htmlFor="name">Name</label>
                        <input className="form-control" placeholder="Name" onChange={event => this.setState({ name: event.target.value })} id="name" name="name" value={this.state.name} required></input>
                    </div>
                    <div className="form-group">
                        <label htmlFor="phone">Telefonnummer</label>
                        <input className="form-control" placeholder="Telefonnummer" onChange={event => this.setState({ phone: event.target.value })} id="phone" name="phone" value={this.state.phone} required></input>
                    </div>
                    <div className="form-group">
                        <label htmlFor="address">Adresse</label>
                        <textarea rows={3} className="form-control" onChange={event => this.setState({ address: event.target.value })} id="address" name="address" value={this.state.address} required></textarea>
                    </div>
                    <button type="submit" className="btn btn-primary">Senden</button>
                </form>
                <div className="mt-2">
                    {this.renderFaxStatus()}
                </div>
            </div>
        );
    }
}
