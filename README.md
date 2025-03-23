# Tiny Ledger: An Assignment for Teya

The original description can be found [here](Teya%20Assessment%20-%20Tiny%20Ledger.pdf); the following is my interpretation.

## Requirements

Build a basic ledger, and offer it as a webservice.

Note that there is no UI expected.

Any persistence should be done in memory.

There is no expectation to include: auth, logging, or transactions.

### API

The following features are expected:

1. Deposit funds into an account.
2. Withdraw funds from an account.
3. View the current balance of an account.
4. View the transaction history

### General notes

I've added a few comments suffixed with `ENHANCEMENT` throughout, indicating what I'd do if it was more than a small assignment.

## Demo

The following is a basic demo of all the functionality; for more details, I'd recommend looking at [the integration tests](src/test/kotlin/com/erasmos/assignments/teya/LedgerApplicationTests.kt).

Note that I took the liberty of adding an endpoint for creating an account, if only for convenience of demonstration.

```
./gradlew bootRun
```

### Creating an account

```
curl --location --request POST 'http://localhost:8080/api/accounts'
```

```json 
{
  "id": "0c412843-4c96-4a14-bd57-815c94d25b5a",
  "currentBalanceInMinorUnits": 0
}
```

### Making a deposit

```
curl --location 'http://localhost:8080/api/accounts/0c412843-4c96-4a14-bd57-815c94d25b5a/deposits' \
--header 'Content-Type: application/json' \
--data '{
"amountInMinorUnits": 1042
}'
```

```json
{
    "id": "5726a6db-739f-43ec-9c00-665b30d200c4",
    "accountId": "0c412843-4c96-4a14-bd57-815c94d25b5a",
    "type": "DEPOSIT",
    "amountInMinorUnits": 1042,
    "date": "2025-03-26T12:27:43.521254"
}
```

### Getting the account info (including current balance)

``` 
curl --location 'http://localhost:8080/api/accounts/0c412843-4c96-4a14-bd57-815c94d25b5a'
```

```json
{
    "id": "0c412843-4c96-4a14-bd57-815c94d25b5a",
    "currentBalanceInMinorUnits": 1042
}
 ```

### Making a withdrawal

```
curl --location 'http://localhost:8080/api/accounts/0c412843-4c96-4a14-bd57-815c94d25b5a/withdrawals' \
--header 'Content-Type: application/json' \
--data '{
"amountInMinorUnits": 200
}'
```

```json
{
  "id": "ce8bc84e-13bf-40fc-9836-5ae5091e1b16",
  "accountId": "0c412843-4c96-4a14-bd57-815c94d25b5a",
  "type": "WITHDRAWAL",
  "amountInMinorUnits": 200,
  "date": "2025-03-26T12:33:20.110316"
}
```

### Listing all transactions

``` 
curl --location 'http://localhost:8080/api/accounts/0c412843-4c96-4a14-bd57-815c94d25b5a/transactions'
```

```json
{
    "accountId": "0c412843-4c96-4a14-bd57-815c94d25b5a",
    "transactions": [
        {
            "id": "5726a6db-739f-43ec-9c00-665b30d200c4",
            "accountId": "0c412843-4c96-4a14-bd57-815c94d25b5a",
            "type": "DEPOSIT",
            "amountInMinorUnits": 1042,
            "date": "2025-03-26T12:27:43.521254"
        },
        {
            "id": "ce8bc84e-13bf-40fc-9836-5ae5091e1b16",
            "accountId": "0c412843-4c96-4a14-bd57-815c94d25b5a",
            "type": "WITHDRAWAL",
            "amountInMinorUnits": 200,
            "date": "2025-03-26T12:33:20.110316"
        }
    ]
}
```

Best regards,
Sean Rasmussen
sean@erasmos.com
