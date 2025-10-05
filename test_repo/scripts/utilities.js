function camelCase(input) {
    return input.toLowerCase().split('_').map((word, index) => {
        if (index === 0) return word;
        return word.charAt(0).toUpperCase() + word.slice(1);
    }).join('');
}

module.exports = { camelCase };