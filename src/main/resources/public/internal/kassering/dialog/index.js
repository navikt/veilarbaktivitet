const form = document.querySelector('form');
const dataContainer = document.getElementById('data');
const henvendelserContainer = document.getElementById('henvendelseContainer');
const fnrInput = document.getElementById('fnr');
const dialogIdInput = document.getElementById('dialogId');

form.addEventListener('submit', handleSok);
document.addEventListener('click', handleClick);

function toHtml(statics, ...values) {
    let str = '';
    statics.forEach((fragment, i) => {
        str += fragment + (values[i] || '');
    });

    const div = document.createElement('tbody');
    div.innerHTML = str;
    return div.children;
}

const tableDialogSetup = `
<table id="table" cellspacing="0" cellpadding="10">
    <tr>
        <th>DialogId</th>
        <th>AntallHenvendelser</th>
        <th>Overskrift</th>
        <th>SisteDato</th>
        <th>Se henvendelser</th>
        <th>Kasser hele dialogen</th>
    </tr>
</table>
`;

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

function handleData(json) {
    dataContainer.innerHTML = tableDialogSetup;
    henvendelserContainer.innerHTML = '';
    const table = document.getElementById('table');
    json
        .sort((d1, d2) => d1.sisteDato.localeCompare(d2.sisteDato))
        .map((dialog) => lagDialog(dialog))
        .forEach((elementer) => Array.from(elementer).forEach((element) => table.appendChild(element)));
}

function lagDialog(dialog) {
    return toHtml`
<tr>
        <td>${dialog.id}</td>
        <td>${dialog.henvendelser.length}</td>
        <td>${dialog.overskrift}</td>
        <td>${dialog.sisteDato}</td>
        <td><button data-dialogid="${dialog.id}" data-action="GET">Se henvendelser</button></td>
        <td><button data-dialogid="${dialog.id}" data-action="DEL">Kasser hele</button></td>
        <td class="hidden-content">
            ${dialog.henvendelser.sort((h1, h2) => h1.sendt.localeCompare(h2.sendt)).map(lagHenvendelse).join('')}
        </td>
</tr>
`;
}

function lagHenvendelse(henvendelse) {
    return `
<div>
<p><b>HenvendelseId:</b>${henvendelse.id}</p>   
<p><b>Avsender:</b>${henvendelse.avsender}</p>
<p><b>AvsenderId:</b>${henvendelse.avsenderId}</p>   
<p><b>Tekst:</b>${henvendelse.tekst}</p>     
<p><b>Dato:</b>${henvendelse.sendt}</p>
<button data-henvendelseid="${henvendelse.id}" data-action="DEL">Kasser henvendelse</button>     
</div>`;
}


function handleSok(event) {
    event.preventDefault();
    handleData([]);

    const fnrValue = fnrInput.value;
    const dialogIdValue = dialogIdInput.value;

    if (dialogIdValue && dialogIdValue.length > 0) {
        fetch(`/veilarbdialog/api/dialog/${dialogIdValue}`, { credentials: 'same-origin' })
            .then(sjekkStatus)
            .then(toJson)
            .then((json) => [json])
            .then(handleData)
            .catch((err) => alert(err));
    } else if (fnrValue && fnrValue.length > 0) {
        fetch(`/veilarbdialog/api/dialog?fnr=${fnrValue}`, { credentials: 'same-origin' })
            .then(sjekkStatus)
            .then(toJson)
            .then(handleData)
            .catch((err) => alert(err));
    } else {
        alert('Du må fylle ut `fnr` eller `dialogId`.');
    }
}

function ask(type, id) {
    if (prompt(`Skriv inn ${type}Id som skal slettes for å verifisere.`) === id) {
        return true;
    } else {
        alert('Feil id...');
        return false;
    }
}

function handleClick(event) {
    const target = event.target;
    const action = target.dataset.action;
    const dialogId = target.dataset.dialogid;
    const henvendelseId = target.dataset.henvendelseid;

    if (!action) {
        return;
    }
    event.preventDefault();

    if (action === 'GET') {
        henvendelserContainer.innerHTML = target.parentElement.parentElement.querySelector('.hidden-content').innerHTML;
    } else if (action === 'DEL') {
        if (dialogId && ask(`Dialog`, dialogId)) {
            handleData([]);
            fetch(`/veilarbdialog/api/kassering/dialog/${dialogId}/kasser`, { credentials: 'same-origin', method: 'PUT' })
                .then(sjekkStatus)
                .then(() => handleSok(event))
                .then(() => alert(`dialog_id: ${dialogId} kassert`))
                .catch((err) => alert(err));

        } else if (henvendelseId && ask(`Henvendelse`, henvendelseId)) {
            handleData([]);
            fetch(`/veilarbdialog/api/kassering/henvendelse/${henvendelseId}/kasser`, { credentials: 'same-origin', method: 'PUT' })
                .then(sjekkStatus)
                .then(() => handleSok(event))
                .then(() => alert(`henvendelse_id: ${henvendelseId} kassert`))
                .catch((err) => alert(err));
        }
    }
}
