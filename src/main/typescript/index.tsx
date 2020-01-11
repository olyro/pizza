import * as React from "react";
import * as ReactDOM from "react-dom";
import { Order, MenuItem } from "../../../generated_types";
import { AdminInterface, calcPrice } from "./admin";

interface PizzaFormProps {
    pizza?: string;
}

interface PizzaFormState {
    order: Order;
    menuItems: Array<MenuItem>
}


export let renderPrice = (price: number) => {
    return (price / 100).toLocaleString('de-DE', { style: 'currency', currency: 'EUR', minimumFractionDigits: 2, maximumFractionDigits: 2 });
}

export class PizzaForm extends React.Component<PizzaFormProps, PizzaFormState> {
    constructor(props: PizzaFormProps) {
        super(props);
        this.state = {
            order: {
                id: "",
                name: "",
                items: [{
                    id: "",
                    extraIds: [],
                    size: ""
                }],
                payed: false
            },
            menuItems: []
        }

        this.updateMenu();
    }

    updateMenu = async () => {
        let menuItems: Array<MenuItem> = await getMenuItems();
        if (menuItems.length > 0) {
            let first = this.props.pizza ? menuItems.find(item => item.item.id === this.props.pizza)! : menuItems[0];
            this.setState({ menuItems: menuItems, order: { ...this.state.order, items: [{ id: first.item.id, extraIds: [], size: first.item.sizes[0].name }] } });
        }
    }

    setName = (event: React.ChangeEvent<HTMLInputElement>) => {
        let newOrder: Order = { ...this.state.order, name: event.target.value };
        this.setState({ order: newOrder });
    };

    setPizza = (event: React.ChangeEvent<HTMLSelectElement>) => {
        let id = event.target.value;
        let menuItem = this.state.menuItems.find(item => item.item.id === id)!;
        let currentSize = this.state.order.items[0].size;
        let size = currentSize && menuItem.item.sizes.find(size => currentSize === size.name) ? currentSize : menuItem.item.sizes[0].name;
        let oldOrdetItem = this.state.order.items[0];
        let newExtras = oldOrdetItem.extraIds.filter(extra => menuItem.extras.map(e => e.id).indexOf(extra) !== -1);
        let newOrder: Order = { ...this.state.order, items: [{ id: event.target.value, extraIds: newExtras, size: size }] };
        this.setState({ order: newOrder });
    };

    setSize = (event: React.ChangeEvent<HTMLSelectElement>) => {
        let size = event.target.value;
        let oldOrdetItem = this.state.order.items[0];
        let newOrder: Order = { ...this.state.order, items: [{ ...oldOrdetItem, size: size }] };
        this.setState({ order: newOrder });
    };

    setExtra = (event: React.ChangeEvent<HTMLInputElement>) => {
        let id = event.target.id;
        let oldOrdetItem = this.state.order.items[0];
        if (event.target.checked) {
            this.setState({ order: { ...this.state.order, items: [{ ...oldOrdetItem, extraIds: [...oldOrdetItem.extraIds, id] }] } });
        } else {
            this.setState({ order: { ...this.state.order, items: [{ ...oldOrdetItem, extraIds: oldOrdetItem.extraIds.filter(eId => eId !== id) }] } });
        }
    }

    sendOrder = async (event: React.FormEvent) => {
        event.preventDefault();
        let response = fetch("/order", { method: "POST", headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(this.state.order) });
        let order: Order = (await (await response).json());
        if (order) {
            window.location.href = `/myorder/${order.id}`;
        }
    };

    renderPizzas = () => {
        return this.state.menuItems.map(item => <option value={item.item.id}>{`${item.item.name} ${item.description}`}</option>);
    }


    renderSizes = () => {
        if (this.state.menuItems.length > 0) {
            let menuItem = this.state.menuItems.find(item => item.item.id === this.state.order.items[0].id) || this.state.menuItems[0];
            return menuItem.item.sizes.map(size => <option value={size.name}>{`${size.name} ${renderPrice(size.price)}`}</option>);
        }
    }

    renderExtras = () => {
        if (this.state.menuItems.length > 0) {
            let menuItem = this.state.menuItems.find(item => item.item.id === this.state.order.items[0].id) || this.state.menuItems[0];
            return menuItem.extras.map(extra => {
                return (
                    <div className="form-check">
                        <input onChange={this.setExtra} className="form-check-input" type="checkbox" name={extra.id} id={extra.id}>
                        </input>
                        <label className="form-check-label" htmlFor={extra.id}>
                            {extra.name}
                        </label>
                    </div>
                )
            });
        }
    }

    render() {
        return (
            <form onSubmit={this.sendOrder}>
                <div className="form-group">
                    <label htmlFor="name">Name/Pseudonym</label>
                    <input value={this.state.order.name} onChange={this.setName} className="form-control" name="name" id="name" required></input>
                </div>
                <div className="form-group">
                    <label htmlFor="item">Pizza</label>
                    <select onChange={this.setPizza} value={this.state.order.items[0].id} className="form-control" id="item" name="item">
                        {this.renderPizzas()}
                    </select>
                </div>
                <div className="form-group">
                    <label htmlFor="size">Größe</label>
                    <select onChange={this.setSize} className="form-control" id="size" name="size">
                        {this.renderSizes()}
                    </select>
                </div>
                <label>Extras</label>
                <div className="max-height">
                    {this.renderExtras()}
                </div>
                <div className="mt-1 mb-1 text-center font-weight-bold">Der Gesamtpreis beträgt {renderPrice(calcPrice(this.state.order, this.state.menuItems))}</div>
                <div className="sending">
                    <button className="btn btn-primary" type="submit">Senden</button>
                </div>
            </form>
        )
    }
}

let getMenuItems = async () => {
    return await (await fetch("/menu")).json();
};

let urlParams = new URLSearchParams(window.location.search);
let reactContainer = document.getElementById("react-container");
let adminContainer = document.getElementById("admin-interface");

if (reactContainer) {
    if (urlParams.has("item")) {
        ReactDOM.render(<PizzaForm pizza={urlParams.get("item")!}></PizzaForm>, reactContainer)
    } else {
        ReactDOM.render(<PizzaForm></PizzaForm>, reactContainer)
    }
}

if (adminContainer && urlParams.has("key")) {
    console.log(urlParams.get("key"));
    ReactDOM.render(<AdminInterface secretKey={urlParams.get("key")!}></AdminInterface>, adminContainer);
}
