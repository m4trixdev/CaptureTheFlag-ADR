# CaptureTheFlag

Minigame de Capture a Bandeira feito para eventos da ADR Studios

![Minecraft](https://img.shields.io/badge/Minecraft-1.21.1-brightgreen)
![Platform](https://img.shields.io/badge/Platform-Paper-blue)
![Language](https://img.shields.io/badge/Linguagem-Java-orange)
![Java Version](https://img.shields.io/badge/Java-21-red)

## 📋 Funcionalidades

- 🚩 **Sistema de Bandeira** - Bandeira é um bloco configurável que dropa como item ao ser destruído
- 📦 **Transporte de Bandeira** - O jogador precisa carregar o item até a zona de entrega do seu time
- 🏁 **Zonas de Entrega** - Cada time tem sua própria zona de entrega configurável por cubóide
- 💀 **Death Zone** - Após o tempo acabar, inicia-se a fase de eliminação total
- 🔄 **Sistema de Revive** - Jogadores mortos podem renascer enquanto a bandeira do seu time estiver viva
- 📊 **Scoreboard Dinâmica** - Atualiza em tempo real mostrando estado, tempo, bandeiras e jogadores vivos
- ✨ **Efeito de Glow** - Portadores de bandeira ficam brilhando com a cor do time inimigo (ativável/desativável)
- 🗺️ **Área de Evento** - Define a região do evento, impedindo saída dos jogadores
- 🎒 **Kits por Time** - Cada time tem seu próprio inventário salvo para distribuição
- 💾 **Persistência de Dados** - Configurações salvas em `data.yml` e recarregadas ao reiniciar

## 🚀 Instalação

### Requisitos

- Java 21+
- Paper 1.21.1+
- Gradle 8+

### Passos de Instalação

1. Baixe o arquivo `CaptureTheFlag-1.0.0.jar` dos releases
2. Coloque o JAR na pasta `plugins` do seu servidor
3. Reinicie o servidor
4. Configure o `config.yml` em `plugins/CaptureTheFlag/`
5. Use os comandos `/evento` para configurar times e área

### Build Manual

```bash
# Clone o repositório
git clone https://github.com/m4trixdev/CaptureTheFlag.git
cd CaptureTheFlag

# Compilar e gerar JAR
./gradlew shadowJar
```

O JAR gerado estará em `build/libs/CaptureTheFlag-1.0.0.jar`

## ⚙️ Configuração

### config.yml

```yaml
event:
  duration: 35          # Duração do evento em minutos
  revive-time: 5        # Tempo de respawn em segundos
  min-players: 2        # Mínimo de jogadores para iniciar
  max-players: 100      # Máximo de jogadores no evento

death-zone:
  enabled: true         # Ativar fase de Death Zone ao fim do tempo

glow:
  enabled: true         # Glow no portador da bandeira inimiga

teams:
  team1:
    name: "&cTime 1"
  team2:
    name: "&9Time 2"

scoreboard:
  title: "&b&lCapture a Bandeira"
  flag-alive: "&a[VIVA]"
  flag-dead: "&c[DESTRUIDA]"
  flag-carried: "&e[ROUBADA]"
  death-zone-label: "&c&lDEATH ZONE"
  starting-label: "&eINICIANDO..."
  lines:
    - "&7Estado: &f%state%"
    - "&7Tempo: &f%time%"
    - " "
    - "%team1%"
    - " &7Bandeira: %flag1%"
    - " &7Vivos: &f%alive1%"
    - "  "
    - "%team2%"
    - " &7Bandeira: %flag2%"
    - " &7Vivos: &f%alive2%"

messages:
  prefix: "&6[Evento]&r "
  event-start: "&aO evento comecou!"
  event-stop: "&cO evento foi encerrado."
  flag-picked-up: "%player% &cpegou a bandeira do %team%&c!"
  flag-dropped: "&eA bandeira do %team% &efoi dropada no chao!"
  flag-returned: "&aA bandeira do %team% &afoi retornada ao lugar!"
  flag-captured: "%player% &acapturou a bandeira do %team%&a!"
  team-win: "%team% &avenceu o evento!"
  revive-countdown: "&eRenascendo em &f%time%s"
  revive-title: "&aRENASCEU!"
  revive-subtitle: "&7Bem-vindo de volta"
  death-zone-start: "&cDeath Zone ativado! Sobreviva!"
  outside-area: "&cVoce nao pode sair da area do evento!"
  countdown: "&a%time%"
```

### Variáveis da Scoreboard

| Variável | Descrição |
|---|---|
| `%state%` | Estado atual do evento |
| `%time%` | Tempo restante (ou DEATH ZONE) |
| `%team1%` | Nome do Time 1 |
| `%team2%` | Nome do Time 2 |
| `%alive1%` | Jogadores vivos no Time 1 |
| `%alive2%` | Jogadores vivos no Time 2 |
| `%flag1%` | Status da bandeira do Time 1 |
| `%flag2%` | Status da bandeira do Time 2 |

## 🎮 Comandos

#### `/evento iniciar`
Inicia o evento, distribui os jogadores em times aleatoriamente e inicia a contagem regressiva.

**Permissão:** `captureflag.start`

---

#### `/evento parar`
Para o evento imediatamente, restaura as bandeiras e devolve os jogadores ao modo normal.

**Permissão:** `captureflag.stop`

---

#### `/evento time1 bandeira`
Define o bloco que será a bandeira do Time 1 (olhe para o bloco desejado).

**Permissão:** `captureflag.set`

---

#### `/evento time1 spawn`
Define o ponto de spawn do Time 1 na sua posição atual.

**Permissão:** `captureflag.set`

---

#### `/evento time1 inventory`
Salva o inventário atual como kit do Time 1.

**Permissão:** `captureflag.set`

---

#### `/evento time1 zona <pos1|pos2>`
Define a zona de entrega do Time 1 (onde o time inimigo deve entregar a bandeira).

**Permissão:** `captureflag.set`

**Exemplo:**
```
/evento time1 zona pos1
/evento time1 zona pos2
```

---

#### `/evento time2 ...`
Todos os subcomandos do `time1` estão disponíveis para `time2`.

---

#### `/evento area <pos1|pos2>`
Define a área global do evento. Jogadores que saírem serão teleportados de volta.

**Permissão:** `captureflag.set`

**Exemplo:**
```
/evento area pos1
/evento area pos2
```

## 🔑 Permissões

| Permissão | Descrição | Padrão |
|---|---|---|
| `captureflag.admin` | Acesso completo ao plugin | OP |
| `captureflag.start` | Permissão para iniciar o evento | OP |
| `captureflag.stop` | Permissão para parar o evento | OP |
| `captureflag.set` | Permissão para configurar times e área | OP |
| `captureflag.bypass` | Não é inserido automaticamente no evento | false |

## 🎯 Mecânicas de Jogo

### Sistema de Bandeira

- A bandeira é um bloco qualquer definido pelo admin via `/evento time1 bandeira`
- Ao ser destruída pelo time inimigo, o bloco **não dropa naturalmente** — ele se torna um **item especial** com PDC (PersistentDataContainer) para identificação segura
- O item cai no lugar do bloco com velocidade zero para evitar que role para fora do alcance

### Coleta e Transporte

- O jogador inimigo que **tocar o item** o recebe automaticamente no inventário
- O portador não pode largar ou mover a bandeira pelo inventário
- Se o portador morrer, a bandeira **retorna imediatamente ao lugar original como bloco** e uma mensagem é enviada a todos

### Zonas de Entrega

- Cada time possui uma zona de entrega definida por dois pontos (cubóide)
- O time deve entregar a **bandeira inimiga** na **zona do seu próprio time**
- Ao entrar na zona com a bandeira no inventário, a captura é validada e o evento termina com vitória

### Retorno da Bandeira

- Se um aliado tocar a bandeira caída no chão, ela **retorna automaticamente** para o bloco original
- Se o portador deslogar, a bandeira também retorna

### Death Zone

- Quando o tempo chega a zero, a Death Zone é ativada
- As bandeiras são removidas e o revive é desabilitado
- A fase termina quando todos os jogadores de um time forem eliminados

### Sistema de Revive

- Ao morrer, o jogador vira espectador e aguarda o tempo de revive
- O revive só ocorre se a **bandeira do seu time ainda estiver viva**
- Se a bandeira foi destruída antes de renascer, o jogador vira espectador permanente

### Efeito de Glow

- Quando um jogador pega a bandeira inimiga, recebe o efeito de **glow** automaticamente
- A cor do glow segue a cor do time dono da bandeira (vermelho para Time 1, azul para Time 2)
- O glow é removido ao capturar, ao morrer ou ao retornar a bandeira
- Pode ser desativado com `glow.enabled: false` no `config.yml`

### Scoreboard Dinâmica

- Atualiza a cada segundo durante a fase RUNNING
- Ao entrar em Death Zone, o campo `%time%` passa a exibir o label configurado em `scoreboard.death-zone-label`
- Linhas duplicadas são tratadas automaticamente com padding invisível para evitar bugs

## 🏗️ Arquitetura

### Estrutura de Pacotes

```
br.com.m4trixdev
├── Main.java
├── command/
│   └── EventoCommand.java
├── config/
│   └── ConfigManager.java
├── data/
│   └── DataManager.java
├── listener/
│   ├── BlockListener.java
│   └── PlayerListener.java
├── manager/
│   ├── EventManager.java
│   └── CTFScoreboardManager.java
├── model/
│   ├── EventArea.java
│   ├── EventState.java
│   └── TeamData.java
└── util/
    ├── ColorUtil.java
    ├── FlagUtil.java
    └── LocationUtil.java
```

### Componentes Principais

```
Main
├── ConfigManager        → Carregamento e gestão de configurações
├── DataManager          → Persistência em data.yml
├── CTFScoreboardManager → Scoreboard dinâmica com glow por time
├── EventManager         → Toda a lógica do evento
└── Listeners
    ├── BlockListener    → Quebra e colocação de blocos
    └── PlayerListener   → Movimentação, morte, respawn e coleta
```

### Estados do Evento

| Estado | Descrição |
|---|---|
| `WAITING` | Aguardando início |
| `STARTING` | Contagem regressiva (5s), jogadores não podem se mover |
| `RUNNING` | Evento em andamento, timer ativo |
| `DEATH_ZONE` | Tempo esgotado, revive desabilitado |
| `ENDING` | Encerrando, limpando dados |

### FlagUtil — Identificação Segura de Bandeiras

As bandeiras usam **PersistentDataContainer (PDC)** com a chave `captureflag:flag_team_id` para identificação. Isso garante que o item seja reconhecido corretamente independentemente do tipo de bloco configurado como bandeira.

## 🛠️ Build

### Requisitos

- JDK 21
- Gradle 8+

### Comandos

```bash
# Compilar
./gradlew compileJava

# Gerar JAR final
./gradlew shadowJar

# Limpar build
./gradlew clean
```

O JAR estará em `build/libs/CaptureTheFlag-1.0.0.jar`

## 🐛 Solução de Problemas

### Evento não inicia

- Verifique se spawn, bandeira, inventory e zona de entrega foram definidos para **ambos os times**
- Verifique se a área global foi definida com `/evento area pos1` e `/evento area pos2`
- Verifique se há jogadores suficientes online (mínimo configurado em `min-players`)
- Revise o console por erros de inicialização

### Bandeira não dropa

- Confirme que o jogador é do time **inimigo** ao tentar quebrar
- Verifique se o evento está no estado `RUNNING`
- Certifique-se de que a bandeira foi configurada com `/evento time1 bandeira`

### Zona de entrega não funciona

- Confirme que `pos1` e `pos2` foram definidos para o time correto
- Verifique se o jogador está carregando a bandeira do **time inimigo** (não do seu time)
- Certifique-se que o jogador está dentro da zona do **seu próprio time**
- Use o mesmo mundo para todos os pontos configurados

### Scoreboard bugada ou não aparece

- Verifique se as `lines` no `config.yml` não possuem linhas totalmente duplicadas (use espaços diferentes como `" "` e `"  "`)
- Use `/evento parar` e reinicie o evento para forçar atualização
- Certifique-se de que o plugin não conflita com outros plugins de scoreboard

### Configuração não carrega

- Verifique se o `config.yml` é um YAML válido
- Apague o `config.yml` e reinicie o servidor para regenerar o padrão
- Revise o console por erros de parsing

## 📄 Licença

Este projeto está licenciado sob a Licença MIT.

## 👨‍💻 Autor

**M4trixDev**

- GitHub: [@m4trixdev](https://github.com/m4trixdev)

## 🤝 Contribuindo

Contribuições são bem-vindas! Sinta-se à vontade para:
- Reportar bugs
- Sugerir novas funcionalidades
- Enviar pull requests
- Melhorar a documentação

## 📞 Suporte

- Issues: [GitHub Issues](https://github.com/m4trixdev/CaptureTheFlag/issues)
- Discussões: [GitHub Discussions](https://github.com/m4trixdev/CaptureTheFlag/discussions)

## 🎮 Servidores Compatíveis

- Paper 1.21.1+
- Qualquer servidor rodando Paper API 1.21+

## ⚠️ Limitações Conhecidas

- Dados do evento são armazenados em memória e limpos ao reiniciar o servidor
- Configurações de times e áreas persistem via `data.yml`
- Algumas funcionalidades podem conflitar com plugins que modificam a scoreboard do jogador
- Requer Paper 1.21.1+ (não compatível com versões anteriores ou Bukkit puro)

---

Feito com ❤️ para eventos da ADR Studios
