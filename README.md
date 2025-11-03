# Software Sign API - ClickSign Integration

API para integração com ClickSign para envio de documentos para assinatura eletrônica.

## Configuração de Papel do Signatário (sign_as)

A partir desta versão, é possível configurar o papel (role) de cada signatário através do campo `sign_as` no JSON da requisição, de acordo com a [documentação da ClickSign](https://developers.clicksign.com/).

### Valores Aceitos para sign_as

- `sign` - Assinante (padrão)
- `approve` - Observador/Aprovador
- `witness` - Testemunha
- `intervening` - Interveniente
- `receipt` - Recibo
- `endorser` - Endossante
- `administrator` - Administrador
- `guarantor` - Garantidor
- `transferor` - Cedente
- `transferee` - Cessionário
- `contractingparty` - Parte Contratante
- `validator` - Validador
- `party` - Parte (genérico)

### Exemplos de Uso

#### 1. Adicionando signatário como Assinante (padrão)

```json
{
  "envelope": {
    "name": "Contrato de Prestação de Serviços"
  },
  "documents": [...],
  "signers": [
    {
      "email": "joao@example.com",
      "name": "João Silva"
    }
  ]
}
```

Quando `sign_as` não é informado, o valor padrão `"sign"` (assinante) é aplicado automaticamente.

#### 2. Adicionando signatário como Observador

```json
{
  "envelope": {
    "name": "Contrato de Prestação de Serviços"
  },
  "documents": [...],
  "signers": [
    {
      "email": "maria@example.com",
      "name": "Maria Santos",
      "sign_as": "approve"
    }
  ]
}
```

#### 3. Adicionando signatário como Testemunha

```json
{
  "envelope": {
    "name": "Contrato de Prestação de Serviços"
  },
  "documents": [...],
  "signers": [
    {
      "email": "pedro@example.com",
      "name": "Pedro Oliveira",
      "sign_as": "witness"
    }
  ]
}
```

#### 4. Múltiplos signatários com diferentes papéis

```json
{
  "envelope": {
    "name": "Contrato de Prestação de Serviços"
  },
  "documents": [...],
  "signers": [
    {
      "email": "contratante@example.com",
      "name": "Empresa Contratante",
      "sign_as": "sign"
    },
    {
      "email": "contratado@example.com",
      "name": "Empresa Contratada",
      "sign_as": "sign"
    },
    {
      "email": "testemunha@example.com",
      "name": "Testemunha Legal",
      "sign_as": "witness"
    },
    {
      "email": "aprovador@example.com",
      "name": "Aprovador Interno",
      "sign_as": "approve"
    }
  ]
}
```

### Endpoint para adicionar signatários

**POST** `/users/envelopes/{envelopeId}/signers`

```json
[
  {
    "email": "exemplo@example.com",
    "name": "Nome do Signatário",
    "sign_as": "sign"
  }
]
```

### Valor Padrão

Se o campo `sign_as` não for informado, o sistema aplicará automaticamente o valor padrão `"sign"` (assinante).

### Notas Importantes

1. O campo `sign_as` é opcional - se omitido, será usado o valor padrão `"sign"`
2. O valor informado deve ser um dos valores aceitos pela API da ClickSign
3. Diferentes signatários no mesmo envelope podem ter diferentes papéis
4. Consulte a [documentação oficial da ClickSign](https://developers.clicksign.com/) para mais detalhes sobre cada tipo de papel

## Mais Informações

Para mais detalhes sobre outros campos e configurações, consulte a documentação completa da API.
