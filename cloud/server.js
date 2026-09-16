const http = require("http");
const crypto = require("crypto");
const WebSocket = require("ws");

const PORT = process.env.PORT || 8080;

const PAIRING_TOKEN =
    process.env.NUVIO_PAIRING_TOKEN;

if (!PAIRING_TOKEN) {
    console.error(
        "NUVIO_PAIRING_TOKEN is required"
    );

    process.exit(1);
}

const server =
    http.createServer(
        (request, response) => {

            if (
                request.url === "/health"
            ) {

                response.writeHead(
                    200,
                    {
                        "Content-Type":
                            "application/json"
                    }
                );

                response.end(
                    JSON.stringify({
                        status: "ok",
                        service:
                            "nuvio-rpc-relay"
                    })
                );

                return;
            }

            response.writeHead(404);

            response.end("Not found");
        }
    );

const websocketServer =
    new WebSocket.Server({
        server
    });

const clients =
    new Set();

function safeEqual(
    a,
    b
) {

    const aBuffer =
        Buffer.from(a || "");

    const bBuffer =
        Buffer.from(b || "");

    if (
        aBuffer.length !==
        bBuffer.length
    ) {
        return false;
    }

    return crypto.timingSafeEqual(
        aBuffer,
        bBuffer
    );
}

websocketServer.on(
    "connection",
    (socket) => {

        const client = {
            socket,
            authenticated: false,
            deviceId: null
        };

        clients.add(client);

        socket.send(
            JSON.stringify({
                type: "hello"
            })
        );

        socket.on(
            "message",
            (raw) => {

                let message;

                try {

                    message =
                        JSON.parse(
                            raw.toString()
                        );

                } catch {

                    return;
                }

                if (
                    !client.authenticated
                ) {

                    if (
                        message.type !==
                        "auth"
                    ) {

                        socket.close(
                            1008,
                            "Authentication required"
                        );

                        return;
                    }

                    const token =
                        message.token;

                    if (
                        !safeEqual(
                            token,
                            PAIRING_TOKEN
                        )
                    ) {

                        socket.close(
                            1008,
                            "Invalid token"
                        );

                        return;
                    }

                    client.authenticated =
                        true;

                    client.deviceId =
                        message.deviceId ||
                        crypto.randomUUID();

                    socket.send(
                        JSON.stringify({
                            type:
                                "authenticated",
                            deviceId:
                                client.deviceId
                        })
                    );

                    return;
                }

                /*
                 * Relay the event to all
                 * other authenticated clients.
                 */
                for (
                    const target of clients
                ) {

                    if (
                        target === client
                    ) {
                        continue;
                    }

                    if (
                        !target.authenticated
                    ) {
                        continue;
                    }

                    if (
                        target.socket.readyState !==
                        WebSocket.OPEN
                    ) {
                        continue;
                    }

                    target.socket.send(
                        raw.toString()
                    );
                }
            }
        );

        socket.on(
            "close",
            () => {
                clients.delete(client);
            }
        );

        socket.on(
            "error",
            () => {
                clients.delete(client);
            }
        );
    }
);

server.listen(
    PORT,
    () => {

        console.log(
            `Nuvio RPC relay listening on ${PORT}`
        );
    }
);
