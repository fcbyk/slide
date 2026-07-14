import threading

import click
import webbrowser
from .utils import (
    check_port,
    copy_to_clipboard,
    echo_network_urls,
    get_private_networks,
    wait_for_server_ready,
)

from .service import SlideService
from .controller import create_slide_app


@click.command(name="slide", help="Start PPT remote control server, control slides via mobile web page")
@click.option(
    "-p",
    "--port",
    default=80,
    help="Web server port (default: 80)",
)
def slide(port):
    while True:
        password = click.prompt(
            "Please set access password",
            hide_input=True,
            confirmation_prompt=True,
        )
        if password:
            break
        click.echo(" Error: Password cannot be empty")

    if not check_port(port):
        return

    click.echo()

    service = SlideService(password)

    app, socketio = create_slide_app(service)
    
    private_networks = get_private_networks()
    local_ip = private_networks[0]["ips"][0]
        
    click.echo(f" PPT Remote Control Server")
    echo_network_urls(private_networks, port, include_virtual=True)
    click.echo(f" Open the URL above on your mobile device to control")

    copy_to_clipboard(f"http://{local_ip}:{port}")
    
    click.echo()

    # 等待服务器就绪后自动打开浏览器
    def _auto_open():
        if wait_for_server_ready(port):
            webbrowser.open(f"http://{local_ip}:{port}")

    threading.Thread(target=_auto_open, daemon=True).start()

    socketio.run(app, host='0.0.0.0', port=port, allow_unsafe_werkzeug=True)

if __name__ == "__main__":
    slide()