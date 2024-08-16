

function downloadPDF(count, start, initials) {
    count = parseInt(count);
    start = parseInt(start);

    // Make AJAX request to your Java backend
    //fetch('/generate-pdf?param=' + param)
    fetch('/generate-pdf?count=' + count + '&start=' + start + '&initials=' + initials)
        .then(response => response.blob())
        .then(blob => {
            // Create a temporary link element
            const link = document.createElement('a');
            link.href = URL.createObjectURL(blob);
            link.download = 'qr_slips.pdf'; // Set the download filename
            // Programmatically click the link
            link.click();
            // Clean up the temporary link
            URL.revokeObjectURL(link.href);
            return false;
        })
        .catch(error => {
            console.error('Error downloading PDF:', error);
        });
}