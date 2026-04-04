# AstroLog API 🔭

API REST para registro e consulta de observações astronômicas.

> **Este projeto é um sample de demonstração do Dev-Lens.**
> Ele contém propositalmente dependências desatualizadas, vulnerabilidades conhecidas
> e cobertura de testes variada para demonstrar as ferramentas de análise.

## Domínio

- **CelestialBody** — corpos celestes catalogados (estrelas, planetas, nebulosas)
- **Observation** — sessões de observação registradas por astrônomos
- **AlertService** — notificações de eventos (chuvas de meteoros, eclipses, conjunções)

## Estrutura

Segue o padrão MVC:

    controller/ → endpoints REST
    service/    → lógica de negócio
    repository/ → acesso a dados
    model/      → entidades