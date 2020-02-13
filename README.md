# RegHelper

Simple microservices using Akka, java8, and play2.7, made to help `REGISTRY` to generate, deposit and download the certificate.


## Installation

Enter the directory of the example and `mvn clean install`

## Verification

- Run Curl Command

``` curl --location --request GET 'http://localhost:9000/echo/12' --header 'Content-Type:application/json' ```

- Response
```{
       "id": null,
       "ver": null,
       "ts": null,
       "params": null,
       "result": {
           "Response": {
               "id": "12"
           }}}