const mock = (data) => new Promise((resolve) => resolve(data));

const form = document.querySelector('form');
const dataContainer = document.getElementById('data');
const aktivitetIdInput = document.getElementById('aktivitetId');

form.addEventListener('submit', handleSok);
document.addEventListener('click', handleClick);

function sjekkStatus(resp) {
    if (!resp.ok) {
        console.log('resp', resp);
        throw new Error(`${resp.status} ${resp.statusText}`);
    }
    return resp;
}

function toJson(resp) {
    return resp.json();
}

function handleData(data) {
    dataContainer.innerHTML = Object.keys(data)
        .map((key) => (
            `<div><b>${key}: </b><span>${data[key]}</span><div>`
        )).join('\n');

    if (data.id) {
        const kasserBtn = document.createElement('button');
        kasserBtn.dataset.aktivitetid = data.id;
        kasserBtn.textContent = 'Kasser';

        dataContainer.appendChild(kasserBtn);
    }
}

function ask(id) {
    if (prompt(`Skriv inn AktivitetId som skal slettes for å verifisere.`) === id) {
        return true;
    } else {
        alert('Feil id...');
        return false;
    }
}

function handleSok(e) {
    e.preventDefault();
    handleData({});

    const aktivitetId = aktivitetIdInput.value;

    if (aktivitetId && aktivitetId.length > 0) {
        fetch(`/veilarbaktivitet/api/kassering/${aktivitetId}`, {credentials: 'same-origin'})
            .then(sjekkStatus)
            .then(toJson)
            .then(handleData)
            .catch((err) => alert(err));
    }
}

function handleClick(event) {
    const target = event.target;
    const aktivitetId = target.dataset.aktivitetid;

    if (aktivitetId && ask(aktivitetId)) {
        handleData({});
        fetch(`/veilarbaktivitet/api/kassering/${aktivitetId}`, { credentials: 'same-origin', method: 'PUT' })
            .then(sjekkStatus)
            .then(() => handleSok(event))
            .then(() => alert(`AktivitetId: ${aktivitetId} kassert`))
            .catch((err) => alert(err));
    }
}
