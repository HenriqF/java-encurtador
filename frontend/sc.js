const input = document.getElementById("link_input");
const botao = document.getElementById("botao");

async function enviar(url) {
    try{
        const r = await fetch("http://localhost:3000/novo", {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify({ url })
        });


        const data = await r.text();
        return data;
    } catch (error){
        console.log(error);
        return "erro...";
    }
}

botao.addEventListener("click", async () => {
    let hash = await enviar(input.value);

    input.value = hash;
});