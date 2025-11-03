# Software Sign API - ClickSign Integration

API para integração com ClickSign para envio de documentos para assinatura eletrônica.

## Configuração de Papel do Signatário

A partir desta versão, é possível configurar o papel (role) de cada signatário através do campo `action` na qualificação (qualification requirement), de acordo com a [documentação da ClickSign](https://developers.clicksign.com/).

### Valores Aceitos para action (Qualification)

- `sign` - Assinante (padrão)
- `approve` - Observador/Aprovador
- `witness` - Testemunha

**Importante**: O papel do signatário NÃO é configurado no objeto `signers`, mas sim no `requirements.qualification.action`.

### Exemplos de Uso

#### 1. Signatário como Assinante (padrão)

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
  ],
  "requirements": {
    "qualification": {
      "enable": true,
      "action": "sign",
      "role": "party"
    }
  }
}
```

Quando `action` não é informado, o valor padrão `"sign"` (assinante) é aplicado automaticamente.

#### 2. Signatário como Observador

```json
{
  "envelope": {
    "name": "Contrato de Prestação de Serviços"
  },
  "documents": [...],
  "signers": [
    {
      "email": "maria@example.com",
      "name": "Maria Santos"
    }
  ],
  "requirements": {
    "qualification": {
      "enable": true,
      "action": "approve",
      "role": "party"
    }
  }
}
```

#### 3. Signatário como Testemunha

```json
{
  "envelope": {
    "name": "Contrato de Prestação de Serviços"
  },
  "documents": [...],
  "signers": [
    {
      "email": "pedro@example.com",
      "name": "Pedro Oliveira"
    }
  ],
  "requirements": {
    "qualification": {
      "enable": true,
      "action": "witness",
      "role": "party"
    }
  }
}
```

#### 4. Múltiplos Signatários (Mesmo Papel)

**Nota**: Quando você tem múltiplos signatários, todos recebem o mesmo `action` configurado em `requirements.qualification`. Se precisar de papéis diferentes para cada signatário, você deve criar envelopes separados ou adicionar os signatários e requisitos individualmente pelos endpoints específicos.

```json
{
  "envelope": {
    "name": "Contrato de Prestação de Serviços"
  },
  "documents": [...],
  "signers": [
    {
      "email": "contratante@example.com",
      "name": "Empresa Contratante"
    },
    {
      "email": "contratado@example.com",
      "name": "Empresa Contratada"
    }
  ],
  "requirements": {
    "qualification": {
      "enable": true,
      "action": "sign",
      "role": "party"
    }
  }
}
```

### Endpoints para Configuração Avançada

Para cenários com múltiplos signatários com papéis diferentes, use os endpoints individuais:

**POST** `/users/envelopes` - Criar envelope

**POST** `/users/envelopes/{envelopeId}/signers` - Adicionar signatários

**POST** `/users/envelopes/{envelopeId}/requirements` - Adicionar requisitos específicos

Exemplo de requisitos com papéis diferentes:

```json
{
  "signers": [
    {
      "signerId": "uuid-signer-1",
      "documentId": "uuid-doc-1",
      "qualification": {
        "enable": true,
        "action": "sign",
        "role": "party"
      }
    },
    {
      "signerId": "uuid-signer-2",
      "documentId": "uuid-doc-1",
      "qualification": {
        "enable": true,
        "action": "witness",
        "role": "party"
      }
    },
    {
      "signerId": "uuid-signer-3",
      "documentId": "uuid-doc-1",
      "qualification": {
        "enable": true,
        "action": "approve",
        "role": "party"
      }
    }
  ]
}
```

### Valor Padrão

Se o campo `action` não for informado em `requirements.qualification`, o sistema aplicará automaticamente o valor padrão `"sign"` (assinante).

### Diferença entre `action` e `role`

- **action**: Define o **tipo de participação** (assinante, observador, testemunha)
- **role**: Define a **qualificação/cargo** da pessoa (party, administrator, guarantor, etc.)

Ambos são enviados no requirement de qualificação (qualification).

### Notas Importantes

1. O campo `action` deve ser configurado em `requirements.qualification.action`, NÃO em `signers`
2. O valor padrão de `action` é `"sign"` (assinante)
3. Todos os signatários no mesmo request receberão o mesmo `action`
4. Para diferentes papéis por signatário, use os endpoints de requirements individuais
5. Consulte a [documentação oficial da ClickSign](https://developers.clicksign.com/) para mais detalhes

## Mais Informações

Para mais detalhes sobre outros campos e configurações, consulte a documentação completa da API.
