# test-rest-sts

Er kun en test app for å konvertere "difi token" til internt oidc token, slik at konsumenter kan teste med et
konvertert "difi token". Dette er kun en dummy. Tjenesten validerer ikke innsendt "difi token", den henter kun ut de
feltene som er spesifisert at skal hentes ut fra difitokenet, og returnerer et nytt signert token. Nøkkel kan hentes ut
fra jwks endepunkt (se OpenIdConnect fasit resource med alias test-rest-sts-oidc).
(Denne har en intern cache for nøkler)

## Endepunkt for konvertering:

i t: https://test-rest-sts-t4.nais.preprod.local/rest/v1/sts/difitoken/exchange
i q: https://test-rest-sts.nais.preprod.local/rest/v1/sts/difitoken/exchange

### Tjenesten forventer:

2 headere: Content-type= application/x-www-form-urlencoded og basic auth header body: token=<encoded "difi token">

<encoded "difi token">: base64 encoded jwt. Disse feltene må være med: "client_orgno", "iss", "aud", "jti".
