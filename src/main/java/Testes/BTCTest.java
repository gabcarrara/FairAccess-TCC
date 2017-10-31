/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Testes;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import static dev_btc.Token.getWalletAddress;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bitcoinj.core.Address;
import org.bitcoinj.core.BlockChain;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.InsufficientMoneyException;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.PeerAddress;
import org.bitcoinj.core.PeerGroup;
import org.bitcoinj.core.ScriptException;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionConfidence;
import org.bitcoinj.core.TransactionInput;
import org.bitcoinj.core.TransactionOutput;
import org.bitcoinj.core.Utils;
import org.bitcoinj.kits.WalletAppKit;
import org.bitcoinj.params.RegTestParams;
import org.bitcoinj.script.Script;
import org.bitcoinj.script.ScriptBuilder;
import org.bitcoinj.script.ScriptOpCodes;
import org.bitcoinj.store.BlockStore;
import org.bitcoinj.store.BlockStoreException;
import org.bitcoinj.store.MemoryBlockStore;
import org.bitcoinj.wallet.KeyChain;
import org.bitcoinj.wallet.SendRequest;
import org.bitcoinj.wallet.UnreadableWalletException;
import org.bitcoinj.wallet.Wallet;
import org.bitcoinj.wallet.listeners.WalletCoinsReceivedEventListener;
import org.bitcoinj.wallet.listeners.WalletCoinsSentEventListener;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.spongycastle.util.encoders.Hex;

/**
 *
 * @author admin_000
 */
public class BTCTest {

    /**
     * @param args the command line arguments
     * @throws org.bitcoinj.store.BlockStoreException
     */
    public static void main(String[] args) throws BlockStoreException {
        NetworkParameters networkParameters = RegTestParams.get();
        File f = new File("wallet");
        Wallet wallet;
        try {
            wallet = Wallet.loadFromFile(f, null);
        } catch (UnreadableWalletException ex) {
            //Logger.getLogger(BTCTest.class.getName()).log(Level.SEVERE, null, ex);
            wallet = new Wallet(networkParameters);
        }

        wallet.autosaveToFile(f, 0, TimeUnit.MINUTES, null);
        BlockStore blockStore = new MemoryBlockStore(networkParameters);
        BlockChain blockChain = new BlockChain(networkParameters, blockStore);
        blockChain.addWallet(wallet);

        PeerGroup peerGroup = new PeerGroup(networkParameters, blockChain);
        peerGroup.setUserAgent("Token App", "0.14");
        peerGroup.addWallet(wallet);

        //Carragando a lista de tokens disponíveis
        final ArrayList<String> tokens = new ArrayList<String>();
        try {
            BufferedReader fl = new BufferedReader(new FileReader("UnusedTokens.dat"));
            while (fl.ready()) {
                tokens.add(fl.readLine());
            }
            fl.close();

        } catch (FileNotFoundException ex) {
            try {
                new File("UnusedTokens.dat").createNewFile();
            } catch (IOException ex1) {
                System.out.println("Não foi possível criar arquivo para salvar tokens criados.");
            }
        } catch (IOException ex) {
            Logger.getLogger(BTCTest.class.getName()).log(Level.SEVERE, null, ex);
        }

        //Adcionando eventos na carteira
        wallet.addCoinsReceivedEventListener(new WalletCoinsReceivedEventListener() {
            public void onCoinsReceived(Wallet wallet, Transaction tx, Coin prevBalance, Coin newBalance) {
                //if (tx.) {
                    
                //}
                
                Transaction txParent = tx.getInputs().get(0).getConnectedOutput().getParentTransaction();
                String tokenText = "";
                System.out.println("");
                System.out.println("*************");
                System.out.println("Token recieved");
                while (tokenText.equals("")) {
                    if (txParent.getOutputs().size() > 1) {
                        tokenText = retrieveToken(txParent.getHashAsString(), wallet);
                        if (tokens.contains(tokenText)) {
                            tokens.remove(tokenText);
                            System.out.println(tokenText);
                            System.out.println("Aceeso permitido");
                        }
                    } else {
                        txParent = txParent.getInputs().get(0).getConnectedOutput().getParentTransaction();
                    }
                }
                System.out.println("*************");
            }
        });

        wallet.addCoinsSentEventListener(new WalletCoinsSentEventListener() {
            public void onCoinsSent(Wallet wallet, Transaction tx, Coin prevBalance, Coin newBalance) {
                System.out.println("Token sent.");
            }
        });

        InetSocketAddress address = new InetSocketAddress("192.168.1.220", 18444);
        PeerAddress add = new PeerAddress(address);

        peerGroup.setUseLocalhostPeerWhenPossible(false);

        peerGroup.start();

        //peerGroup.connectToLocalHost();
        //peerGroup.connectTo(address);
        peerGroup.addAddress(add);

        System.out.println("Started");
        peerGroup.downloadBlockChain();

        System.out.println("BlockChain downloaded");

        System.out.println(peerGroup.getConnectedPeers());
        System.out.println("Completo " + blockChain.getBestChainHeight());
        System.out.println("Send Address: " + wallet.currentReceiveAddress());

        //System.out.println(wallet.getWatchedOutputs(true));
        //System.out.println("Endereços assistidos: " + wallet.getWatchedAddresses());
        System.out.println("Endereços: " + wallet.getIssuedReceiveAddresses());
        System.out.println("Moedas atuais " + wallet.getBalance(Wallet.BalanceType.AVAILABLE).toFriendlyString());
        // System.out.println(wallet.getTransactions(false).iterator().next());

        Scanner sc = new Scanner(System.in);

        String sendAddress;
        boolean done = false;
        while (!done) {
            System.out.println("Escolha sua opção:\n 1 - Enviar\n 2 - Enviar Token\n 3 - Buscar Token\n 4 - Verificar ID");
            System.out.print("\\>");
            sendAddress = "";
            switch (sc.nextInt()) {
                case 1:
                    System.out.println("Quanto quer enviar?");
                    System.out.print("\\>");
                    int qtd = sc.nextInt();
                    System.out.println("Qual endereço?");
                    System.out.print("\\>");
                    sendAddress = sc.next();
                    send(qtd, sendAddress, wallet, networkParameters);
                    break;
                case 2:
                    System.out.println("O que quer enviar?");
                    System.out.print("\\>");
                    String token = sc.next();
                    System.out.println("Qual endereço?");
                    System.out.print("\\>");
                    sendAddress = sc.next();
                    if (sendToken(token, sendAddress, wallet, networkParameters)) {
                        tokens.add(token);
                    }
                    break;
                case 3:
                    System.out.println("Qual transação?");
                    System.out.print("\\>");
                    String txId = sc.next();
                    System.out.println(retrieveToken(txId, wallet));
                    break;
                case 4:
                    String t = "";
                    System.out.println("Qual transação?");
                    System.out.print("\\>");
                    String id = sc.next();
                    Transaction tx = wallet.getTransaction(Sha256Hash.wrap(id));
                    TransactionOutput out = tx.getInputs().get(0).getConnectedOutput();

                    if (out != null) {
                        Transaction parentTx = out.getParentTransaction();
                        //System.out.println(parentTx.getHashAsString());
                        t = retrieveToken(parentTx.getHashAsString(), wallet);
                    }

                    if (t.equals("")) {
                        System.out.println("Token not found!");
                    } else {
                        System.out.println(t);
                    }

                    break;

                case 5:
                    System.out.println("Qual transação?");
                    System.out.print("\\>");
                    String idTx = sc.next();
                     {
                        try {
                            String result = getJSONFromTransaction(wallet.getTransaction(Sha256Hash.wrap(idTx)), networkParameters, wallet);
                            File file = new File("Transaction " + idTx + ".txt");
                            FileWriter fw = new FileWriter(file);
                            fw.write(result);
                            fw.close();
                        } catch (ScriptException ex) {
                            Logger.getLogger(BTCTest.class.getName()).log(Level.SEVERE, null, ex);
                        } catch (JSONException ex) {
                            Logger.getLogger(BTCTest.class.getName()).log(Level.SEVERE, null, ex);
                        } catch (IOException ex) {
                            Logger.getLogger(BTCTest.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                    break;
                case 6:
                    for (TransactionOutput to : wallet.calculateAllSpendCandidates(true, true)) {
                        System.out.println(to);
                    }
                    for (String s : tokens) {
                        System.out.println(s);
                    }
                    break;
                case 0: {
                    try {
                        BufferedWriter fw = new BufferedWriter(new FileWriter("UnusedTokens.dat"));
                        for (String tkn : tokens) {
                            fw.write(tkn);
                            fw.newLine();
                        }
                        fw.close();

                    } catch (IOException ex) {
                        Logger.getLogger(BTCTest.class.getName()).log(Level.SEVERE, null, ex);
                        System.out.println("Não foi possivel salvar tokens em arquivo.");
                    }
                }
                done = true;
                break;
                default:
                    System.out.println("Sem opções, tente novamente.");
            }
        }
        try {
            wallet.saveToFile(f);
        } catch (IOException ex) {
            //Logger.getLogger(BTCTest.class.getName()).log(Level.SEVERE, null, ex);
            System.out.println("Carteira não pode ser salva.");
        }

        peerGroup.stop();

    }

    private static void send(int qtd, String sendAddress, Wallet wallet, NetworkParameters networkParameters) {
        Transaction tx = new Transaction(networkParameters);
        tx.addOutput(Coin.valueOf(qtd, 0), Address.fromBase58(networkParameters, sendAddress));

        try {
            wallet.sendCoins(SendRequest.forTx(tx));

        } catch (InsufficientMoneyException ex) {
            Logger.getLogger(BTCTest.class.getName()).log(Level.SEVERE, null, ex);
            System.out.println("Erro - Sem moedas suficientes para transação.");
        }

        final ListenableFuture<Coin> balanceFuture = wallet.getBalanceFuture(Coin.valueOf(qtd, 0), Wallet.BalanceType.AVAILABLE);
        FutureCallback<Coin> callback = new FutureCallback<Coin>() {
            public void onSuccess(Coin balance) {
                System.out.println("Moedas enviadas com sucesso!");

            }

            public void onFailure(Throwable t) {
                System.out.println("Moedas não puderam ser enviadas, por favor tentar novamente mais tarde.");
            }
        };
        Futures.addCallback(balanceFuture, callback);
        System.out.println("Moedas atuais " + wallet.getBalance(Wallet.BalanceType.AVAILABLE_SPENDABLE).toFriendlyString());

    }

    private static boolean sendToken(String token, String sendAddress, Wallet wallet, NetworkParameters networkParameters) {
        Transaction tx = new Transaction(networkParameters);
        Script opReturn = new ScriptBuilder().op(ScriptOpCodes.OP_RETURN).data(token.getBytes()).build();
        tx.addOutput(Coin.parseCoin("1"), Address.fromBase58(networkParameters, sendAddress));
        tx.addOutput(Coin.ZERO, opReturn);
        List<TransactionOutput> unspent = wallet.getUnspents();
        for (TransactionOutput transactionOutput : unspent) {
            if (transactionOutput.getValue().isGreaterThan(Coin.parseCoin("1"))) {
                tx.addSignedInput(transactionOutput, wallet.currentKey(KeyChain.KeyPurpose.AUTHENTICATION));
                break;
            }
        }

        try {
            wallet.sendCoins(SendRequest.forTx(tx));

            System.out.println("Token enviado com sucesso");

        } catch (InsufficientMoneyException ex) {
            Logger.getLogger(BTCTest.class.getName()).log(Level.SEVERE, null, ex);
            System.out.println("Erro - Sem moedas suficientes para transação.");
            return false;
        }

        System.out.println("Moedas atuais " + wallet.getBalance(Wallet.BalanceType.AVAILABLE_SPENDABLE).toFriendlyString());
        return true;
    }

    private static String retrieveToken(String txId, Wallet w) {
        String resp = "";
        Sha256Hash id = Sha256Hash.wrap(txId);
        Transaction tx = w.getTransaction(id);
        String txS = tx.toString();

        System.out.println(txS);
        if (txS.contains("RETURN PUSHDATA")) {
            String token;
            String mydata = tx.getOutputs().toString(); //you could use out.getScriptPubKey()
            String[] parti = mydata.split("RETURN PUSHDATA" + "\\((.*?)\\)");
            String part = parti[1];

            Pattern pattern = Pattern.compile("\\[(.*?)\\]");
            Matcher matcher = pattern.matcher(part);
            if (matcher.find()) {
                token = matcher.group(0).replace("[", "");
                token = token.replace("]", "");
                //System.out.println(token);

                resp = new String(Hex.decode(token), StandardCharsets.UTF_8);
                //System.out.println(resp);

            }
        }
        return resp;
    }

    private static String getJSONFromTransaction(Transaction tx, NetworkParameters networkParams, Wallet wallet) throws ScriptException, JSONException {
        if (tx == null) {
            return null;
        }

        TransactionConfidence txConfidence = tx.getConfidence();
        TransactionConfidence.ConfidenceType confidenceType = txConfidence.getConfidenceType();
        String confidence;

        if (confidenceType == TransactionConfidence.ConfidenceType.BUILDING) {
            confidence = "building";
        } else if (confidenceType == TransactionConfidence.ConfidenceType.PENDING) {
            confidence = "pending";
        } else if (confidenceType == TransactionConfidence.ConfidenceType.DEAD) {
            confidence = "dead";
        } else {
            confidence = "unknown";
        }

        JSONArray inputs = new JSONArray();

        for (TransactionInput input : tx.getInputs()) {
            JSONObject inputData = new JSONObject();

            if (!input.isCoinBase()) {
                try {
                    Script scriptSig = input.getScriptSig();
                    Address fromAddress = new Address(networkParams, Utils.sha256hash160(scriptSig.getPubKey()));
                    inputData.put("address", fromAddress);
                } catch (ScriptException e) {
                    // can't parse script, give up
                }
            }

            TransactionOutput source = input.getConnectedOutput();
            if (source != null) {
                inputData.put("amount", source.getValue());
            }

            inputs.put(inputData);
        }

        JSONArray outputs = new JSONArray();

        for (TransactionOutput output : tx.getOutputs()) {
            JSONObject outputData = new JSONObject();

            try {
                Script scriptPubKey = output.getScriptPubKey();

                if (scriptPubKey.isSentToAddress() || scriptPubKey.isPayToScriptHash()) {
                    Address toAddress = scriptPubKey.getToAddress(networkParams);
                    outputData.put("address", toAddress);

                    if (toAddress.toString().equals(getWalletAddress(networkParams, wallet))) {
                        outputData.put("type", "own");
                    } else {
                        outputData.put("type", "external");
                    }
                } else if (scriptPubKey.isSentToRawPubKey()) {
                    outputData.put("type", "pubkey");
                } else if (scriptPubKey.isSentToMultiSig()) {
                    outputData.put("type", "multisig");
                } else {
                    outputData.put("type", "unknown");
                }
            } catch (ScriptException e) {
                // can't parse script, give up
            }

            outputData.put("amount", output.getValue());
            outputs.put(outputData);
        }

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));

        JSONObject result = new JSONObject();
        result.put("amount", tx.getValue(wallet));
        // result.put("fee", getTransactionFee(tx));
        result.put("txid", tx.getHashAsString());
        result.put("time", dateFormat.format(tx.getUpdateTime()));
        result.put("confidence", confidence);
        result.put("peers", txConfidence.numBroadcastPeers());
        result.put("confirmations", txConfidence.getDepthInBlocks());
        result.put("inputs", inputs);
        result.put("outputs", outputs);

        return result.toString();
    }

    private static String[] getWalletAddress(NetworkParameters networkParams, Wallet wallet) {
        int i = 0, lenght = 0;
        ECKey ecKey;
        List<ECKey> list_key = wallet.getIssuedReceiveKeys();
        for (ECKey k : list_key) {
            lenght++;
        }
        String[] Addres = new String[lenght];

        for (ECKey k : list_key) {
            ecKey = wallet.getIssuedReceiveKeys().get(i);// .getKeys().get(0);
            // System.out.println(" Addresses :"+ecKey.toAddress(networkParams).toString());

            Addres[i] = ecKey.toAddress(networkParams).toString();
            i++;
        }
        return Addres;
    }
}
