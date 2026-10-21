# Manual de acesso remoto (VPN)

## Instalação
O cliente de VPN da empresa é o FortiClient. Baixe pelo portal de software (software.acme.local) e escolha o perfil "ACME-Remoto". Notebooks corporativos já vêm com o cliente instalado.

## Conectando
Use o endereço vpn.acme.com.br na porta 443. O usuário é o mesmo do Windows, sem o domínio. Depois da senha, o aplicativo Microsoft Authenticator pede a aprovação do segundo fator.

## Erros comuns
**Erro de autenticação (código -12):** a senha do domínio expirou ou foi digitada errada. Troque a senha pelo portal senha.acme.local e tente de novo depois de 5 minutos, que é o tempo de sincronização.

**Conecta e cai em seguida:** normalmente é a rede doméstica bloqueando a porta 443 para UDP. Em Configurações, desmarque "Usar DTLS" para forçar TCP.

**Não acessa sistemas internos mesmo conectado:** o DNS interno não foi aplicado. Desconecte, rode `ipconfig /flushdns` e conecte de novo.

Se nada resolver, abra um chamado na categoria ACESSO com o print da mensagem de erro.
